package com.smartgrid.vendorservice.web;

import com.smartgrid.vendorservice.domain.Vendor;
import com.smartgrid.vendorservice.domain.VendorHealthReport;
import com.smartgrid.vendorservice.dto.CreateVendorRequest;
import com.smartgrid.vendorservice.dto.UpdateRatingRequest;
import com.smartgrid.vendorservice.dto.VendorHealthReportRequest;
import com.smartgrid.vendorservice.dto.VendorHealthReportResponse;
import com.smartgrid.vendorservice.dto.VendorRankingResult;
import com.smartgrid.vendorservice.dto.VendorResponse;
import com.smartgrid.vendorservice.dto.VendorSkuResponse;
import com.smartgrid.vendorservice.repository.VendorHealthReportRepository;
import com.smartgrid.vendorservice.repository.VendorSkuRepository;
import com.smartgrid.vendorservice.service.VendorScoreService;
import com.smartgrid.vendorservice.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final VendorScoreService vendorScoreService;
    private final VendorSkuRepository vendorSkuRepository;
    private final VendorHealthReportRepository vendorHealthReportRepository;

    public VendorController(VendorService vendorService, VendorScoreService vendorScoreService,
                            VendorSkuRepository vendorSkuRepository,
                            VendorHealthReportRepository vendorHealthReportRepository) {
        this.vendorService = vendorService;
        this.vendorScoreService = vendorScoreService;
        this.vendorSkuRepository = vendorSkuRepository;
        this.vendorHealthReportRepository = vendorHealthReportRepository;
    }

    @GetMapping
    public List<VendorResponse> listVendors() {
        return vendorService.listVendors();
    }

    @PostMapping
    public ResponseEntity<VendorResponse> createVendor(@Valid @RequestBody CreateVendorRequest request) {
        Vendor vendor = vendorService.createVendor(request);
        double reliability = vendorService.getReliabilityScore(vendor.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(VendorResponse.from(vendor, reliability));
    }

    @GetMapping("/{id}")
    public VendorResponse getVendor(@PathVariable UUID id) {
        return vendorService.getVendorResponse(id);
    }

    @GetMapping("/{id}/skus")
    public List<VendorSkuResponse> skusForVendor(@PathVariable UUID id) {
        return vendorSkuRepository.findByVendor_Id(id).stream().map(VendorSkuResponse::from).toList();
    }

    @PostMapping("/{id}/health-reports")
    public ResponseEntity<VendorHealthReportResponse> createHealthReport(@PathVariable UUID id, @Valid @RequestBody VendorHealthReportRequest request) {
        VendorHealthReport report = new VendorHealthReport(id, request.trendDirection(), request.trendSlope(),
                request.averageLeadTimeDays(), request.breachCount(), request.summary());
        vendorHealthReportRepository.save(report);
        vendorScoreService.applyHealthReportAdjustment(id, request.trendDirection());
        return ResponseEntity.status(HttpStatus.CREATED).body(VendorHealthReportResponse.from(report));
    }

    @GetMapping("/{id}/health-reports")
    public List<VendorHealthReportResponse> healthReports(@PathVariable UUID id) {
        return vendorHealthReportRepository.findByVendorIdOrderByCreatedAtDesc(id).stream()
                .map(VendorHealthReportResponse::from)
                .toList();
    }

    @GetMapping("/top")
    public ResponseEntity<List<VendorRankingResult>> topVendors(
            @RequestParam String sku,
            @RequestParam(defaultValue = "5") int k
    ) {
        long start = System.currentTimeMillis();
        List<VendorRankingResult> results = vendorService.topVendorsForSku(sku, k);
        long duration = System.currentTimeMillis() - start;
        return ResponseEntity.ok()
                .header("X-Algorithm-Latency-Ms", String.valueOf(duration))
                .header("X-Algorithm-Complexity", "O(N log K) Min-Heap")
                .body(results);
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<Void> updateRating(@PathVariable UUID id, @Valid @RequestBody UpdateRatingRequest request) {
        vendorScoreService.setReliabilityScore(id, request.reliabilityScore(), "Manual rating update");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<VendorResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String certification
    ) {
        return vendorService.searchVendors(q, category, region, certification);
    }
}
