package com.smartgrid.vendorservice.messaging;

import com.smartgrid.commons.avro.order.OrderCreated;
import com.smartgrid.commons.avro.order.OrderLineItem;
import com.smartgrid.commons.avro.sla.SLABreached;
import com.smartgrid.vendorservice.domain.SkuDemandCounter;
import com.smartgrid.vendorservice.dto.VendorRankingResult;
import com.smartgrid.vendorservice.repository.SkuDemandCounterRepository;
import com.smartgrid.vendorservice.service.VendorScoreService;
import com.smartgrid.vendorservice.service.VendorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class VendorEventListener {

    private static final Logger log = LoggerFactory.getLogger(VendorEventListener.class);

    private final SkuDemandCounterRepository skuDemandCounterRepository;
    private final VendorScoreService vendorScoreService;
    private final VendorService vendorService;
    private final VendorEventPublisher vendorEventPublisher;

    public VendorEventListener(SkuDemandCounterRepository skuDemandCounterRepository, VendorScoreService vendorScoreService,
                                VendorService vendorService, VendorEventPublisher vendorEventPublisher) {
        this.skuDemandCounterRepository = skuDemandCounterRepository;
        this.vendorScoreService = vendorScoreService;
        this.vendorService = vendorService;
        this.vendorEventPublisher = vendorEventPublisher;
    }

    @KafkaListener(topics = "order-events", groupId = "${smartgrid.kafka.group-id}-orders")
    @Transactional
    public void onOrderEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof OrderCreated created) {
            for (OrderLineItem item : created.getItems()) {
                SkuDemandCounter counter = skuDemandCounterRepository.findById(item.getSkuId())
                        .orElseGet(() -> new SkuDemandCounter(item.getSkuId(), 0));
                counter.addQuantity(item.getQuantity());
                skuDemandCounterRepository.save(counter);
            }
            confirmVendorForOrder(created);
        }
    }

    /**
     * order-service's saga waits for a VendorConfirmed event on vendor-events before an order can
     * leave PENDING (see OrderSagaListener/OrderService.onVendorConfirmed) - nothing produced that
     * event anywhere in the system until now. Real orders have one vendor, so this picks the
     * top-ranked vendor for the order's first line item's SKU, the same ranking the "top vendors by
     * SKU" endpoint already uses, and confirms it for the whole order.
     */
    private void confirmVendorForOrder(OrderCreated created) {
        if (created.getItems().isEmpty()) {
            return;
        }
        OrderLineItem primaryItem = created.getItems().get(0);
        List<VendorRankingResult> ranked = vendorService.topVendorsForSku(primaryItem.getSkuId(), 1);
        if (ranked.isEmpty()) {
            log.warn("No vendor available for order={} sku={}; VendorConfirmed not published", created.getOrderId(), primaryItem.getSkuId());
            return;
        }
        VendorRankingResult best = ranked.get(0);
        vendorEventPublisher.publishVendorConfirmed(created.getOrderId(), best.vendorId().toString(),
                primaryItem.getSkuId(), primaryItem.getQuantity());
    }

    @KafkaListener(topics = "sla-events", groupId = "${smartgrid.kafka.group-id}-sla")
    public void onSlaEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof SLABreached breached) {
            UUID vendorId;
            try {
                vendorId = UUID.fromString(breached.getVendorId());
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring SLABreached with non-UUID vendorId={}", breached.getVendorId());
                return;
            }
            vendorScoreService.applySlaBreachPenalty(vendorId, breached.getSeverity());
        }
    }
}
