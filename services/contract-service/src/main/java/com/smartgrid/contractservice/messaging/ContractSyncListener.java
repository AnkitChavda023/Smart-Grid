package com.smartgrid.contractservice.messaging;

import com.smartgrid.commons.avro.contract.ContractIndexed;
import com.smartgrid.contractservice.search.ContractDocument;
import com.smartgrid.contractservice.search.ContractDocumentRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ContractSyncListener {

    private final ContractDocumentRepository contractDocumentRepository;

    public ContractSyncListener(ContractDocumentRepository contractDocumentRepository) {
        this.contractDocumentRepository = contractDocumentRepository;
    }

    @KafkaListener(topics = "contract-sync-events", groupId = "${smartgrid.kafka.group-id}-sync")
    public void onContractIndexed(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof ContractIndexed indexed) {
            contractDocumentRepository.save(new ContractDocument(
                    indexed.getContractId(), indexed.getVendorId(), indexed.getTerms(),
                    indexed.getStartDate(), indexed.getEndDate(), indexed.getActive()));
        }
    }
}
