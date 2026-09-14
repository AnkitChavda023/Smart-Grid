package com.smartgrid.contractservice.repository;

import com.smartgrid.contractservice.domain.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PenaltyRepository extends JpaRepository<Penalty, UUID> {

    List<Penalty> findByContractId(UUID contractId);
}
