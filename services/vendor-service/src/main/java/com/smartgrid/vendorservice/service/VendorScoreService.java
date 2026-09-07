package com.smartgrid.vendorservice.service;

import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.vendorservice.domain.Vendor;
import com.smartgrid.vendorservice.domain.VendorScore;
import com.smartgrid.vendorservice.messaging.VendorEventPublisher;
import com.smartgrid.vendorservice.repository.VendorRepository;
import com.smartgrid.vendorservice.repository.VendorScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class VendorScoreService {

    private static final double SUSPENSION_THRESHOLD = 0.2;

    private final VendorRepository vendorRepository;
    private final VendorScoreRepository vendorScoreRepository;
    private final VendorEventPublisher publisher;
    private final VendorRankingCacheService cacheService;

    public VendorScoreService(
            VendorRepository vendorRepository,
            VendorScoreRepository vendorScoreRepository,
            VendorEventPublisher publisher,
            VendorRankingCacheService cacheService
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorScoreRepository = vendorScoreRepository;
        this.publisher = publisher;
        this.cacheService = cacheService;
    }

    @Transactional
    public void setReliabilityScore(UUID vendorId, double newReliability, String reason) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId.toString()));
        applyScoreChange(vendor, clamp(newReliability), reason);
    }

    /** Updates vendor reliability score based on the trend direction from a health report. */
    @Transactional
    public void applyHealthReportAdjustment(UUID vendorId, String trendDirection) {
        Vendor vendor = vendorRepository.findById(vendorId).orElse(null);
        if (vendor == null) {
            return;
        }
        double delta = switch (trendDirection == null ? "" : trendDirection.toUpperCase()) {
            case "IMPROVING" -> 0.05;
            case "DECLINING" -> -0.05;
            default -> 0.0;
        };
        if (delta == 0.0) {
            return;
        }
        VendorScore score = vendorScoreRepository.findByVendorId(vendorId)
                .orElseGet(() -> new VendorScore(vendorId, 1.0));
        double target = clamp(score.getReliabilityScore() + delta);
        applyScoreChange(vendor, target, "Vendor health report: " + trendDirection);
    }

    @Transactional
    public void applySlaBreachPenalty(UUID vendorId, String severity) {
        Vendor vendor = vendorRepository.findById(vendorId).orElse(null);
        if (vendor == null) {
            return;
        }
        double penalty = switch (severity == null ? "" : severity.toUpperCase()) {
            case "CRITICAL" -> 0.40;
            case "HIGH" -> 0.25;
            case "MEDIUM" -> 0.15;
            default -> 0.05;
        };
        VendorScore score = vendorScoreRepository.findByVendorId(vendorId)
                .orElseGet(() -> new VendorScore(vendorId, 1.0));
        double target = clamp(score.getReliabilityScore() - penalty);
        applyScoreChange(vendor, target, "SLA breach (" + severity + ")");
    }

    private void applyScoreChange(Vendor vendor, double newReliability, String reason) {
        VendorScore score = vendorScoreRepository.findByVendorId(vendor.getId())
                .orElseGet(() -> new VendorScore(vendor.getId(), 1.0));
        double previous = score.getReliabilityScore();
        score.setReliabilityScore(newReliability);
        vendorScoreRepository.save(score);

        cacheService.invalidateAll();
        publisher.publishScoreUpdated(vendor.getId().toString(), null, previous, newReliability, reason);

        if (newReliability < SUSPENSION_THRESHOLD && !vendor.isSuspended()) {
            vendor.setSuspended(true);
            publisher.publishSuspended(vendor.getId().toString(), "Reliability score fell below suspension threshold");
        }
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
