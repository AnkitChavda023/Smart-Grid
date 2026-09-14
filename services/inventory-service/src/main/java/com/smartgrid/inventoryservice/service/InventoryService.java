package com.smartgrid.inventoryservice.service;

import com.smartgrid.commons.exception.ConflictException;
import com.smartgrid.inventoryservice.domain.InventoryEvent;
import com.smartgrid.inventoryservice.domain.InventoryEventType;
import com.smartgrid.inventoryservice.domain.InventorySnapshot;
import com.smartgrid.inventoryservice.messaging.InventoryEventPublisher;
import com.smartgrid.inventoryservice.repository.InventoryEventRepository;
import com.smartgrid.inventoryservice.repository.InventorySnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private static final long SNAPSHOT_LAG_THRESHOLD = 1000;

    private final InventoryEventRepository eventRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final InventoryEventPublisher publisher;

    public InventoryService(
            InventoryEventRepository eventRepository,
            InventorySnapshotRepository snapshotRepository,
            InventoryEventPublisher publisher
    ) {
        this.eventRepository = eventRepository;
        this.snapshotRepository = snapshotRepository;
        this.publisher = publisher;
    }

    @Transactional
    public void replenish(String skuId, String warehouseId, long quantity) {
        InventorySnapshot snapshot = loadForUpdate(skuId, warehouseId);
        InventoryEvent event = eventRepository.save(
                new InventoryEvent(skuId, warehouseId, InventoryEventType.REPLENISHED, quantity, null));
        snapshot.apply(quantity, event.getId());
        snapshotRepository.save(snapshot);

        publisher.publishStockReplenished(skuId, warehouseId, quantity);
    }

    @Transactional
    public void reserve(String orderId, String skuId, String warehouseId, long quantity) {
        InventorySnapshot snapshot = loadForUpdate(skuId, warehouseId);
        if (snapshot.getAvailableQuantity() < quantity) {
            throw new ConflictException(
                    "Insufficient stock for sku " + skuId + " at warehouse " + warehouseId
                            + ": requested " + quantity + ", available " + snapshot.getAvailableQuantity());
        }

        InventoryEvent event = eventRepository.save(
                new InventoryEvent(skuId, warehouseId, InventoryEventType.RESERVED, -quantity, orderId));
        snapshot.apply(-quantity, event.getId());
        snapshotRepository.save(snapshot);

        publisher.publishReservationConfirmed(orderId, skuId, quantity, warehouseId);
        if (snapshot.getAvailableQuantity() == 0) {
            publisher.publishStockDepleted(skuId, warehouseId);
        }
    }

    @Transactional
    public void release(String orderId, String skuId, String warehouseId, long quantity) {
        InventorySnapshot snapshot = loadForUpdate(skuId, warehouseId);
        InventoryEvent event = eventRepository.save(
                new InventoryEvent(skuId, warehouseId, InventoryEventType.RELEASED, quantity, orderId));
        snapshot.apply(quantity, event.getId());
        snapshotRepository.save(snapshot);
    }

    @Transactional
    public void releaseAllForOrder(String orderId) {
        List<InventoryEvent> reservations = eventRepository.findByOrderIdAndEventType(orderId, InventoryEventType.RESERVED);
        for (InventoryEvent reservation : reservations) {
            release(orderId, reservation.getSkuId(), reservation.getWarehouseId(), -reservation.getQuantityDelta());
        }
    }

    @Transactional(readOnly = true)
    public long totalAvailable(String skuId) {
        List<String> warehouses = snapshotRepository.findBySkuId(skuId).stream()
                .map(InventorySnapshot::getWarehouseId)
                .distinct()
                .toList();
        if (warehouses.isEmpty()) {
            return 0;
        }
        return warehouses.stream()
                .mapToLong(w -> replayAvailableQuantity(skuId, w))
                .sum();
    }

    @Transactional(readOnly = true)
    public List<InventorySnapshot> snapshotsForSku(String skuId) {
        List<InventorySnapshot> snapshots = snapshotRepository.findBySkuId(skuId);
        for (InventorySnapshot s : snapshots) {
            long recomputed = replayAvailableQuantity(s.getSkuId(), s.getWarehouseId());
            s.replaceWith(recomputed, s.getLastEventId());
        }
        return snapshots;
    }

    @Transactional(readOnly = true)
    public List<String> allSkuIds() {
        return snapshotRepository.findDistinctSkuIds();
    }

    @Transactional(readOnly = true)
    public List<InventoryEvent> demandEventsForSku(String skuId) {
        return eventRepository.findBySkuIdAndEventTypeOrderByCreatedAtAsc(skuId, InventoryEventType.RESERVED);
    }

    @Transactional(readOnly = true)
    public List<InventorySnapshot> warehouseInventory(String warehouseId) {
        List<InventorySnapshot> snapshots = snapshotRepository.findByWarehouseId(warehouseId);
        for (InventorySnapshot s : snapshots) {
            long recomputed = replayAvailableQuantity(s.getSkuId(), s.getWarehouseId());
            s.replaceWith(recomputed, s.getLastEventId());
        }
        return snapshots;
    }

    @Transactional
    public void updateSafetyStockLevel(String skuId, String warehouseId, long safetyStockLevel) {
        InventorySnapshot snapshot = snapshotRepository.findBySkuIdAndWarehouseId(skuId, warehouseId)
                .orElseGet(() -> snapshotRepository.save(new InventorySnapshot(skuId, warehouseId, 0, 0)));
        snapshot.setSafetyStockLevel(safetyStockLevel);
        snapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public long replayAvailableQuantity(String skuId, String warehouseId) {
        return eventRepository.findBySkuIdAndWarehouseIdOrderByIdAsc(skuId, warehouseId).stream()
                .mapToLong(InventoryEvent::getQuantityDelta)
                .sum();
    }

    // SELECT ... FOR UPDATE serializes concurrent reserve/release on this key; always computes available quantity from the immutable event log.
    private InventorySnapshot loadForUpdate(String skuId, String warehouseId) {
        InventorySnapshot snapshot = snapshotRepository.findForUpdate(skuId, warehouseId)
                .orElseGet(() -> snapshotRepository.save(new InventorySnapshot(skuId, warehouseId, 0, 0)));

        long recomputed = replayAvailableQuantity(skuId, warehouseId);
        Long maxEventId = eventRepository.findMaxEventId(skuId, warehouseId);
        snapshot.replaceWith(recomputed, maxEventId == null ? 0 : maxEventId);
        return snapshot;
    }
}
