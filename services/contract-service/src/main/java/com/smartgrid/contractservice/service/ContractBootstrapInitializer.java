package com.smartgrid.contractservice.service;

import com.smartgrid.contractservice.domain.Contract;
import com.smartgrid.contractservice.domain.SlaTerm;
import com.smartgrid.contractservice.repository.ContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Bootstrap runner ensuring all active vendors have an active contract with
 * the three mandatory SLA terms per PR-07:
 * 1. Maximum lead time (max_lead_time_days)
 * 2. Minimum on-time delivery % (min_on_time_delivery_pct)
 * 3. Maximum consecutive delays allowed (max_consecutive_delays)
 */
@Component
public class ContractBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContractBootstrapInitializer.class);

    private final ContractRepository contractRepository;

    public ContractBootstrapInitializer(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureTermsOnExistingContracts();
    }

    public void ensureTermsOnExistingContracts() {
        List<Contract> activeContracts = contractRepository.findByActiveTrue();
        for (Contract contract : activeContracts) {
            boolean hasLeadTime = false;
            boolean hasOnTimePct = false;
            boolean hasConsecutive = false;

            for (SlaTerm term : contract.getSlaTerms()) {
                String name = term.getMetricName().toLowerCase();
                if (name.contains("lead_time")) hasLeadTime = true;
                if (name.contains("on_time") || name.contains("ontime")) hasOnTimePct = true;
                if (name.contains("consecutive")) hasConsecutive = true;
            }

            boolean modified = false;
            if (!hasLeadTime) {
                contract.addSlaTerm(new SlaTerm("max_lead_time_days", 5.0, 500.0));
                modified = true;
            }
            if (!hasOnTimePct) {
                contract.addSlaTerm(new SlaTerm("min_on_time_delivery_pct", 95.0, 1000.0));
                modified = true;
            }
            if (!hasConsecutive) {
                contract.addSlaTerm(new SlaTerm("max_consecutive_delays", 2.0, 1500.0));
                modified = true;
            }

            if (modified) {
                contractRepository.save(contract);
                log.info("Initialized mandatory SLA terms for vendor contract: {}", contract.getVendorId());
            }
        }
    }

    @Transactional
    public Contract ensureVendorHasContract(String vendorId) {
        List<Contract> existing = contractRepository.findByVendorIdAndActiveTrue(vendorId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        Contract contract = new Contract(
                vendorId,
                "Standard SmartGrid Supplier SLA Agreement. Maximum lead time 5 days, 95% on-time delivery commitment, max 2 consecutive delays.",
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );
        contract.addSlaTerm(new SlaTerm("max_lead_time_days", 5.0, 500.0));
        contract.addSlaTerm(new SlaTerm("min_on_time_delivery_pct", 95.0, 1000.0));
        contract.addSlaTerm(new SlaTerm("max_consecutive_delays", 2.0, 1500.0));
        Contract saved = contractRepository.save(contract);
        log.info("Provisioned new active SLA contract with all 3 mandatory terms for vendor: {}", vendorId);
        return saved;
    }
}
