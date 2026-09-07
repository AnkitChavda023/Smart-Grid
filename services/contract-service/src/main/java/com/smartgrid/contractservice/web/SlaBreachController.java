package com.smartgrid.contractservice.web;

import com.smartgrid.contractservice.dto.SlaBreachResponse;
import com.smartgrid.contractservice.repository.SlaBreachRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SlaBreachController {

    private final SlaBreachRepository slaBreachRepository;

    public SlaBreachController(SlaBreachRepository slaBreachRepository) {
        this.slaBreachRepository = slaBreachRepository;
    }

    @GetMapping("/sla-breaches")
    public List<SlaBreachResponse> breaches(@RequestParam String vendorId) {
        return slaBreachRepository.findByVendorId(vendorId).stream().map(SlaBreachResponse::from).toList();
    }
}
