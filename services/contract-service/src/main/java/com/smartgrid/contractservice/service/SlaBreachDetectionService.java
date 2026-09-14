package com.smartgrid.contractservice.service;

import com.smartgrid.contractservice.domain.Contract;
import com.smartgrid.contractservice.domain.Penalty;
import com.smartgrid.contractservice.domain.SlaBreach;
import com.smartgrid.contractservice.domain.SlaTerm;
import com.smartgrid.contractservice.messaging.ContractEventPublisher;
import com.smartgrid.contractservice.repository.ContractRepository;
import com.smartgrid.contractservice.repository.PenaltyRepository;
import com.smartgrid.contractservice.repository.SlaBreachRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SlaBreachDetectionService {

    public static final String METRIC_MAX_LEAD_TIME = "max_lead_time_days";
    public static final String METRIC_MIN_ON_TIME_DELIVERY = "min_on_time_delivery_pct";
    public static final String METRIC_MAX_CONSECUTIVE_DELAYS = "max_consecutive_delays";

    private final ContractRepository contractRepository;
    private final SlaBreachRepository slaBreachRepository;
    private final PenaltyRepository penaltyRepository;
    private final ContractEventPublisher publisher;

    public SlaBreachDetectionService(
            ContractRepository contractRepository,
            SlaBreachRepository slaBreachRepository,
            PenaltyRepository penaltyRepository,
            ContractEventPublisher publisher
    ) {
        this.contractRepository = contractRepository;
        this.slaBreachRepository = slaBreachRepository;
        this.penaltyRepository = penaltyRepository;
        this.publisher = publisher;
    }

    @Transactional
    public void evaluateLeadTime(String vendorId, double actualLeadTimeDays, String reason) {
        List<Contract> activeContracts = contractRepository.findByVendorIdAndActiveTrue(vendorId);
        for (Contract contract : activeContracts) {
            for (SlaTerm term : contract.getSlaTerms()) {
                String m = term.getMetricName().toLowerCase();
                if (m.contains("lead_time")) {
                    if (actualLeadTimeDays > term.getThresholdValue()) {
                        recordBreach(contract, term.getThresholdValue(), actualLeadTimeDays, term.getPenaltyPerBreach(), reason);
                    }
                }
            }
        }
    }

    @Transactional
    public void evaluateOnTimeDelivery(String vendorId, double actualPct, String reason) {
        List<Contract> activeContracts = contractRepository.findByVendorIdAndActiveTrue(vendorId);
        for (Contract contract : activeContracts) {
            for (SlaTerm term : contract.getSlaTerms()) {
                String m = term.getMetricName().toLowerCase();
                if (m.contains("on_time") || m.contains("ontime")) {
                    if (actualPct < term.getThresholdValue()) {
                        recordBreach(contract, term.getThresholdValue(), actualPct, term.getPenaltyPerBreach(), reason);
                    }
                }
            }
        }
    }

    @Transactional
    public void evaluateConsecutiveDelays(String vendorId, int actualConsecutiveDelays, String reason) {
        List<Contract> activeContracts = contractRepository.findByVendorIdAndActiveTrue(vendorId);
        for (Contract contract : activeContracts) {
            for (SlaTerm term : contract.getSlaTerms()) {
                String m = term.getMetricName().toLowerCase();
                if (m.contains("consecutive")) {
                    if (actualConsecutiveDelays > term.getThresholdValue()) {
                        recordBreach(contract, term.getThresholdValue(), actualConsecutiveDelays, term.getPenaltyPerBreach(), reason);
                    }
                }
            }
        }
    }

    private void recordBreach(Contract contract, double threshold, double actualValue, double basePenalty, String reason) {
        double delayMagnitude = Math.abs(actualValue - threshold);
        long recentBreachFrequency = slaBreachRepository.countByVendorId(contract.getVendorId());

        double score = delayMagnitude * 1.0 + recentBreachFrequency * 0.5;
        String severity = severityFor(score);
        double penaltyAmount = basePenalty * severityMultiplier(severity);

        SlaBreach breach = slaBreachRepository.save(new SlaBreach(contract.getVendorId(), contract.getId(), severity, penaltyAmount, reason));
        publisher.publishSlaBreached(breach);

        Penalty penalty = penaltyRepository.save(new Penalty(contract.getId(), contract.getVendorId(), penaltyAmount));
        publisher.publishPenaltyComputed(contract.getId(), contract.getVendorId(), penalty.getAmount());
    }

    private String severityFor(double score) {
        if (score >= 10) {
            return "CRITICAL";
        }
        if (score >= 5) {
            return "HIGH";
        }
        if (score >= 2) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private double severityMultiplier(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 3.0;
            case "HIGH" -> 2.0;
            case "MEDIUM" -> 1.5;
            default -> 1.0;
        };
    }
}
