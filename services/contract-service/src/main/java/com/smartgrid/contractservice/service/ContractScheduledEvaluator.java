package com.smartgrid.contractservice.service;

import com.smartgrid.contractservice.domain.Contract;
import com.smartgrid.contractservice.domain.VendorDeliveryRecord;
import com.smartgrid.contractservice.messaging.ContractEventPublisher;
import com.smartgrid.contractservice.repository.ContractRepository;
import com.smartgrid.contractservice.repository.VendorDeliveryRecordRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ContractScheduledEvaluator {

    private static final int EXPIRY_WARNING_DAYS = 7;

    private final ContractRepository contractRepository;
    private final VendorDeliveryRecordRepository deliveryRecordRepository;
    private final SlaBreachDetectionService slaBreachDetectionService;
    private final ContractEventPublisher publisher;

    public ContractScheduledEvaluator(
            ContractRepository contractRepository,
            VendorDeliveryRecordRepository deliveryRecordRepository,
            SlaBreachDetectionService slaBreachDetectionService,
            ContractEventPublisher publisher
    ) {
        this.contractRepository = contractRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.slaBreachDetectionService = slaBreachDetectionService;
        this.publisher = publisher;
    }

    // Evaluates all active contracts every 15 minutes (900,000 ms) automatically per PR-07
    @Scheduled(fixedRateString = "${smartgrid.contract.evaluator-interval-ms:900000}")
    @Transactional
    public void evaluate() {
        try (Stream<Contract> activeContracts = contractRepository.streamByActiveTrue()) {
            activeContracts.forEach(this::evaluateContract);
        }
    }

    public void evaluateContract(Contract contract) {
        checkExpiry(contract);
        checkOverdueDeliveries(contract);
        checkOnTimeDelivery(contract);
        checkConsecutiveDelays(contract);
    }

    private void checkExpiry(Contract contract) {
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());
        if (daysRemaining >= 0 && daysRemaining <= EXPIRY_WARNING_DAYS && !contract.isExpiryNotified()) {
            contract.setExpiryNotified(true);
            contractRepository.save(contract);
            publisher.publishContractExpiring(contract, (int) daysRemaining);
        }
    }

    private void checkOverdueDeliveries(Contract contract) {
        contract.getSlaTerms().stream()
                .filter(term -> term.getMetricName().toLowerCase().contains("lead_time"))
                .findFirst()
                .ifPresent(term -> {
                    List<VendorDeliveryRecord> pending = deliveryRecordRepository.findByDeliveredFalse();
                    for (VendorDeliveryRecord record : pending) {
                        if (!record.getVendorId().equals(contract.getVendorId())) {
                            continue;
                        }
                        double elapsedDays = Duration.between(record.getFulfilledAt(), Instant.now()).toMillis() / 86_400_000.0;
                        if (elapsedDays > term.getThresholdValue()) {
                            slaBreachDetectionService.evaluateLeadTime(contract.getVendorId(), elapsedDays,
                                    "Order " + record.getOrderId() + " overdue for delivery (" + String.format("%.1f", elapsedDays) + " day(s) elapsed)");
                        }
                    }
                });
    }

    private void checkOnTimeDelivery(Contract contract) {
        contract.getSlaTerms().stream()
                .filter(term -> term.getMetricName().toLowerCase().contains("on_time") || term.getMetricName().toLowerCase().contains("ontime"))
                .findFirst()
                .ifPresent(term -> {
                    List<VendorDeliveryRecord> records = deliveryRecordRepository.findByVendorId(contract.getVendorId());
                    if (records.isEmpty()) {
                        return;
                    }
                    long onTimeCount = records.stream()
                            .filter(VendorDeliveryRecord::isDelivered)
                            .count();
                    double actualPct = (double) onTimeCount / records.size() * 100.0;
                    if (actualPct < term.getThresholdValue()) {
                        slaBreachDetectionService.evaluateOnTimeDelivery(contract.getVendorId(), actualPct,
                                "On-time delivery percentage fell to " + String.format("%.1f", actualPct) + "% (threshold: " + term.getThresholdValue() + "%)");
                    }
                });
    }

    private void checkConsecutiveDelays(Contract contract) {
        contract.getSlaTerms().stream()
                .filter(term -> term.getMetricName().toLowerCase().contains("consecutive"))
                .findFirst()
                .ifPresent(term -> {
                    List<VendorDeliveryRecord> records = deliveryRecordRepository.findByVendorId(contract.getVendorId());
                    if (records.isEmpty()) {
                        return;
                    }
                    int consecutiveDelays = 0;
                    int maxConsecutive = 0;
                    for (VendorDeliveryRecord r : records) {
                        if (!r.isDelivered()) {
                            consecutiveDelays++;
                            if (consecutiveDelays > maxConsecutive) {
                                maxConsecutive = consecutiveDelays;
                            }
                        } else {
                            consecutiveDelays = 0;
                        }
                    }
                    if (maxConsecutive > term.getThresholdValue()) {
                        slaBreachDetectionService.evaluateConsecutiveDelays(contract.getVendorId(), maxConsecutive,
                                "Vendor experienced " + maxConsecutive + " consecutive delayed shipments (threshold: " + (int) term.getThresholdValue() + ")");
                    }
                });
    }
}
