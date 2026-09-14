package com.smartgrid.vendorservice.service;

import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.vendorservice.domain.Vendor;
import com.smartgrid.vendorservice.domain.VendorScore;
import com.smartgrid.vendorservice.domain.VendorSku;
import com.smartgrid.vendorservice.dto.CreateVendorRequest;
import com.smartgrid.vendorservice.dto.VendorRankingResult;
import com.smartgrid.vendorservice.dto.VendorResponse;
import com.smartgrid.vendorservice.dto.VendorSkuRequest;
import com.smartgrid.vendorservice.repository.VendorRepository;
import com.smartgrid.vendorservice.repository.VendorScoreRepository;
import com.smartgrid.vendorservice.search.VendorDocument;
import com.smartgrid.vendorservice.search.VendorSearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VendorService {

    private static final double INITIAL_RELIABILITY_SCORE = 1.0;

    private final VendorRepository vendorRepository;
    private final VendorScoreRepository vendorScoreRepository;
    private final VendorSearchService vendorSearchService;
    private final VendorRankingService vendorRankingService;
    private final VendorRankingCacheService vendorRankingCacheService;

    public VendorService(
            VendorRepository vendorRepository,
            VendorScoreRepository vendorScoreRepository,
            VendorSearchService vendorSearchService,
            VendorRankingService vendorRankingService,
            VendorRankingCacheService vendorRankingCacheService
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorScoreRepository = vendorScoreRepository;
        this.vendorSearchService = vendorSearchService;
        this.vendorRankingService = vendorRankingService;
        this.vendorRankingCacheService = vendorRankingCacheService;
    }

    public List<VendorRankingResult> topVendorsForSku(String skuId, int k) {
        return vendorRankingCacheService.get(skuId, k)
                .orElseGet(() -> {
                    List<VendorRankingResult> computed = vendorRankingService.topK(skuId, k);
                    vendorRankingCacheService.put(skuId, k, computed);
                    return computed;
                });
    }

    @Transactional
    public Vendor createVendor(CreateVendorRequest request) {
        Vendor vendor = new Vendor(
                request.name(),
                request.region(),
                request.latitude(),
                request.longitude(),
                request.capabilities(),
                request.contact(),
                request.category(),
                request.certifications()
        );
        List<VendorSkuRequest> skus = request.skus() == null ? List.of() : request.skus();
        for (VendorSkuRequest skuRequest : skus) {
            vendor.addSku(new VendorSku(skuRequest.skuId(), skuRequest.price(), skuRequest.leadTimeDays()));
        }
        Vendor saved = vendorRepository.save(vendor);
        vendorScoreRepository.save(new VendorScore(saved.getId(), INITIAL_RELIABILITY_SCORE));
        try {
            vendorSearchService.index(saved);
        } catch (Exception ignored) {
        }
        vendorRankingCacheService.invalidateAll();
        return saved;
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> listVendors() {
        return vendorRepository.findAll().stream()
                .map(v -> VendorResponse.from(v, getReliabilityScore(v.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Vendor getVendor(UUID id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", id.toString()));
    }

    @Transactional(readOnly = true)
    public VendorResponse getVendorResponse(UUID id) {
        Vendor vendor = getVendor(id);
        return VendorResponse.from(vendor, getReliabilityScore(id));
    }

    @Transactional(readOnly = true)
    public double getReliabilityScore(UUID vendorId) {
        return vendorScoreRepository.findByVendorId(vendorId)
                .map(VendorScore::getReliabilityScore)
                .orElse(INITIAL_RELIABILITY_SCORE);
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> searchVendors(String query, String category, String region, String certification) {
        List<Vendor> all = vendorRepository.findAll();
        return all.stream()
                .filter(v -> {
                    if (query != null && !query.isBlank()) {
                        String q = query.toLowerCase().trim();
                        boolean matchName = v.getName() != null && v.getName().toLowerCase().contains(q);
                        boolean matchCap = v.getCapabilities() != null && v.getCapabilities().toLowerCase().contains(q);
                        boolean matchReg = v.getRegion() != null && v.getRegion().toLowerCase().contains(q);
                        boolean matchCat = v.getCategory() != null && v.getCategory().toLowerCase().contains(q);
                        boolean matchCert = v.getCertifications() != null && v.getCertifications().toLowerCase().contains(q);
                        if (!matchName && !matchCap && !matchReg && !matchCat && !matchCert) return false;
                    }
                    if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
                        if (v.getCategory() == null || !v.getCategory().equalsIgnoreCase(category.trim())) return false;
                    }
                    if (region != null && !region.isBlank() && !"all".equalsIgnoreCase(region)) {
                        if (v.getRegion() == null || !v.getRegion().equalsIgnoreCase(region.trim())) return false;
                    }
                    if (certification != null && !certification.isBlank()) {
                        if (v.getCertifications() == null || !v.getCertifications().toLowerCase().contains(certification.toLowerCase().trim())) return false;
                    }
                    return true;
                })
                .map(v -> VendorResponse.from(v, getReliabilityScore(v.getId())))
                .toList();
    }
}
