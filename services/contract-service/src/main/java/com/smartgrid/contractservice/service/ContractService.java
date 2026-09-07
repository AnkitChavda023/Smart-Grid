package com.smartgrid.contractservice.service;

import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.contractservice.domain.Contract;
import com.smartgrid.contractservice.domain.ContractDraft;
import com.smartgrid.contractservice.domain.SlaTerm;
import com.smartgrid.contractservice.dto.CreateContractRequest;
import com.smartgrid.contractservice.dto.SlaTermRequest;
import com.smartgrid.contractservice.messaging.ContractSyncPublisher;
import com.smartgrid.contractservice.repository.ContractDraftRepository;
import com.smartgrid.contractservice.repository.ContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractDraftRepository contractDraftRepository;
    private final ContractSyncPublisher syncPublisher;

    public ContractService(ContractRepository contractRepository, ContractDraftRepository contractDraftRepository,
                            ContractSyncPublisher syncPublisher) {
        this.contractRepository = contractRepository;
        this.contractDraftRepository = contractDraftRepository;
        this.syncPublisher = syncPublisher;
    }

    @Transactional
    public Contract createContract(CreateContractRequest request) {
        Contract contract = new Contract(request.vendorId(), request.terms(), request.startDate(), request.endDate());
        for (SlaTermRequest termRequest : request.slaTerms()) {
            contract.addSlaTerm(new SlaTerm(termRequest.metricName(), termRequest.thresholdValue(), termRequest.penaltyPerBreach()));
        }
        Contract saved = contractRepository.save(contract);
        syncPublisher.publishIndexed(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Contract> activeContractsForVendor(String vendorId) {
        return contractRepository.findByVendorIdAndActiveTrue(vendorId);
    }

    @Transactional(readOnly = true)
    public List<Contract> listAll() {
        return contractRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Contract getContract(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", id.toString()));
    }

    @Transactional
    public ContractDraft saveDraft(String vendorId, UUID existingContractId, String proposedTerms, String summary) {
        return contractDraftRepository.save(new ContractDraft(vendorId, existingContractId, proposedTerms, summary));
    }

    @Transactional(readOnly = true)
    public ContractDraft getDraft(UUID id) {
        return contractDraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContractDraft", id.toString()));
    }

    @Transactional(readOnly = true)
    public List<ContractDraft> listDrafts() {
        return contractDraftRepository.findAll();
    }

    @Transactional
    public ContractDraft rejectDraft(UUID id) {
        ContractDraft draft = getDraft(id);
        draft.markRejected();
        return contractDraftRepository.save(draft);
    }

    @Transactional
    public ContractDraft modifyDraft(UUID id, String proposedTerms, String summary) {
        ContractDraft draft = getDraft(id);
        draft.updateProposedTerms(proposedTerms, summary);
        return contractDraftRepository.save(draft);
    }

    /**
     * The only path that turns a draft into a real, active contract — reachable exclusively via
     * {@code POST /contracts/drafts/{id}/submit}, gated to PLANNER/ADMIN JWTs at the controller.
     * There is deliberately no MCP tool that calls this. SLA terms carry forward from the contract
     * being renewed, if any — a draft's free-text proposedTerms has no structured metric/threshold
     * data of its own to build new SlaTerm rows from.
     */
    @Transactional
    public Contract submitDraft(UUID draftId) {
        ContractDraft draft = getDraft(draftId);
        Contract contract = new Contract(draft.getVendorId(), draft.getProposedTerms(), LocalDate.now(), LocalDate.now().plusYears(1));
        if (draft.getExistingContractId() != null) {
            contractRepository.findById(draft.getExistingContractId()).ifPresent(existing ->
                    existing.getSlaTerms().forEach(term ->
                            contract.addSlaTerm(new SlaTerm(term.getMetricName(), term.getThresholdValue(), term.getPenaltyPerBreach()))));
        }
        Contract saved = contractRepository.save(contract);
        syncPublisher.publishIndexed(saved);
        draft.markSubmitted();
        contractDraftRepository.save(draft);
        return saved;
    }
}
