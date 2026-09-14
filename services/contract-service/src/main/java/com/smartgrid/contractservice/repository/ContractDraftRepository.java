package com.smartgrid.contractservice.repository;

import com.smartgrid.contractservice.domain.ContractDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContractDraftRepository extends JpaRepository<ContractDraft, UUID> {
}
