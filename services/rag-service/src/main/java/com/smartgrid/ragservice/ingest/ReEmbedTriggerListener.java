package com.smartgrid.ragservice.ingest;

import com.smartgrid.commons.avro.sla.ContractExpiring;
import com.smartgrid.commons.avro.vendor.VendorScoreUpdated;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Component
public class ReEmbedTriggerListener {

    private static final Logger log = LoggerFactory.getLogger(ReEmbedTriggerListener.class);

    private final IngestionService ingestionService;
    private final VendorSourceClient vendorClient;
    private final ContractSourceClient contractClient;

    public ReEmbedTriggerListener(IngestionService ingestionService, VendorSourceClient vendorClient, ContractSourceClient contractClient) {
        this.ingestionService = ingestionService;
        this.vendorClient = vendorClient;
        this.contractClient = contractClient;
    }

    @KafkaListener(topics = "vendor-events", groupId = "${smartgrid.kafka.group-id}-vendor")
    public void onVendorEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof VendorScoreUpdated event) {
            UUID vendorId = UUID.fromString(event.getVendorId());
            VendorSourceClient.VendorCatalogEntry vendor = vendorClient.getById(vendorId);
            if (vendor != null) {
                ingestionService.ingestVendorCapability(vendor.id(), vendor.capabilities());
                log.info("Re-embedded vendor capability chunk for vendorId={} after VendorScoreUpdated", vendor.id());
            }
            for (ContractSourceClient.SlaBreachRecord breach : contractClient.breachesForVendor(event.getVendorId())) {
                ingestionService.ingestBreach(breach.id().toString(), breach.vendorId(), breach.reason());
            }
        }
    }

    @KafkaListener(topics = "sla-events", groupId = "${smartgrid.kafka.group-id}-sla")
    public void onSlaEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof ContractExpiring event) {
            for (ContractSourceClient.ContractRecord contract : contractClient.activeForVendor(event.getVendorId())) {
                ingestionService.ingestContractTerms(contract.id().toString(), contract.vendorId(), contract.terms());
            }
            log.info("Re-embedded active contract chunks for vendorId={} after ContractExpiring", event.getVendorId());
        }
    }
}
