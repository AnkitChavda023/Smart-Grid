package com.smartgrid.vendorevaluatoragent.repository;

import com.smartgrid.vendorevaluatoragent.domain.VendorLeadTimeObservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VendorLeadTimeObservationRepository extends JpaRepository<VendorLeadTimeObservation, UUID> {
    List<VendorLeadTimeObservation> findByVendorIdAndObservedAtAfterOrderByObservedAtAsc(String vendorId, Instant since);
}
