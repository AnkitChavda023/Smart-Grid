package com.smartgrid.contractservice.repository;

import com.smartgrid.contractservice.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    List<Contract> findByVendorIdAndActiveTrue(String vendorId);

    List<Contract> findByActiveTrue();

    // A true DB cursor (fetch-size hinted, streamed) rather than loading every active contract into memory at once.
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "50"))
    Stream<Contract> streamByActiveTrue();
}
