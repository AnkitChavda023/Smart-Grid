package com.smartgrid.vendorservice.service;

import com.smartgrid.vendorservice.domain.VendorScore;
import com.smartgrid.vendorservice.domain.VendorSku;
import com.smartgrid.vendorservice.dto.VendorRankingResult;
import com.smartgrid.vendorservice.repository.VendorScoreRepository;
import com.smartgrid.vendorservice.repository.VendorSkuRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class VendorRankingService {

    private static final double PRICE_WEIGHT = 0.40;
    private static final double LEAD_TIME_WEIGHT = 0.35;
    private static final double RELIABILITY_WEIGHT = 0.25;
    private static final double DEFAULT_RELIABILITY = 1.0;

    private final VendorSkuRepository vendorSkuRepository;
    private final VendorScoreRepository vendorScoreRepository;

    public VendorRankingService(VendorSkuRepository vendorSkuRepository, VendorScoreRepository vendorScoreRepository) {
        this.vendorSkuRepository = vendorSkuRepository;
        this.vendorScoreRepository = vendorScoreRepository;
    }

    public List<VendorRankingResult> topK(String skuId, int k) {
        List<VendorSku> candidates = vendorSkuRepository.findBySkuIdAndVendor_SuspendedFalse(skuId);
        if (candidates.isEmpty()) {
            return List.of();
        }

        double minPrice = candidates.stream().mapToDouble(VendorSku::getPrice).min().orElse(0);
        double maxPrice = candidates.stream().mapToDouble(VendorSku::getPrice).max().orElse(0);
        double minLeadTime = candidates.stream().mapToInt(VendorSku::getLeadTimeDays).min().orElse(0);
        double maxLeadTime = candidates.stream().mapToInt(VendorSku::getLeadTimeDays).max().orElse(0);

        // Min-heap of size k: keeps the k candidates with the highest composite score in O(n log k).
        PriorityQueue<VendorRankingResult> minHeap = new PriorityQueue<>(Comparator.comparingDouble(VendorRankingResult::compositeScore));

        for (VendorSku sku : candidates) {
            double reliability = vendorScoreRepository.findByVendorId(sku.getVendor().getId())
                    .map(VendorScore::getReliabilityScore)
                    .orElse(DEFAULT_RELIABILITY);

            double priceScore = normalizeInverse(sku.getPrice(), minPrice, maxPrice);
            double leadTimeScore = normalizeInverse(sku.getLeadTimeDays(), minLeadTime, maxLeadTime);
            double composite = priceScore * PRICE_WEIGHT + leadTimeScore * LEAD_TIME_WEIGHT + reliability * RELIABILITY_WEIGHT;

            VendorRankingResult candidate = new VendorRankingResult(
                    sku.getVendor().getId(), sku.getVendor().getName(), composite,
                    sku.getPrice(), sku.getLeadTimeDays(), reliability
            );

            minHeap.offer(candidate);
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        List<VendorRankingResult> result = new ArrayList<>(minHeap);
        result.sort(Comparator.comparingDouble(VendorRankingResult::compositeScore).reversed());
        return result;
    }

    private double normalizeInverse(double value, double min, double max) {
        if (max == min) {
            return 1.0;
        }
        return 1.0 - ((value - min) / (max - min));
    }
}
