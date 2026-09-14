package com.smartgrid.ragservice;

import com.smartgrid.commons.avro.vendor.VendorScoreUpdated;
import com.smartgrid.ragservice.domain.SourceType;
import com.smartgrid.ragservice.ingest.IngestionService;
import com.smartgrid.ragservice.repository.DocumentChunkRepository;
import com.smartgrid.ragservice.retrieval.HybridRetrievalService;
import com.smartgrid.ragservice.web.RagController;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RagServiceTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:18081";

    // Apex Grid Transformers â€” real vendor seeded via a direct POST /vendors call ahead of this test run.
    private static final String SEEDED_VENDOR_ID = "f4edbdae-6c1a-4497-807a-fb2a6e347ba2";

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private HybridRetrievalService retrievalService;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    private String vendorAId;
    private String vendorBId;
    private String vendorCId;

    @BeforeEach
    void ingestDistinctVendors() {
        vendorAId = "it-vendor-" + UUID.randomUUID();
        vendorBId = "it-vendor-" + UUID.randomUUID();
        vendorCId = "it-vendor-" + UUID.randomUUID();

        ingestionService.ingestVendorCapability(vendorAId,
                "Manufactures underground fiber optic cable and conduit for telecom trenching projects in dense urban corridors.");
        ingestionService.ingestVendorCapability(vendorBId,
                "Provides emergency helicopter search and rescue services for offshore oil platforms and remote wilderness areas.");
        ingestionService.ingestVendorCapability(vendorCId,
                "Supplies industrial bakery ovens and dough mixing equipment for commercial food production facilities.");
    }

    @Test
    void hybridSearchRanksSemanticallyRelevantVendorFirst() {
        List<HybridRetrievalService.RetrievedChunk> results = retrievalService.search(
                "who can bury cable lines for telecommunications under city streets?", 5);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).sourceId()).isEqualTo(vendorAId);
        assertThat(results.stream().map(HybridRetrievalService.RetrievedChunk::sourceId))
                .doesNotContain(vendorBId, vendorCId);
    }

    @Test
    void hybridSearchDistinguishesUnrelatedDomains() {
        List<HybridRetrievalService.RetrievedChunk> rescueResults = retrievalService.search(
                "aerial rescue teams for stranded offshore platform workers", 5);
        List<HybridRetrievalService.RetrievedChunk> bakeryResults = retrievalService.search(
                "ovens for a commercial bread bakery", 5);

        assertThat(rescueResults.get(0).sourceId()).isEqualTo(vendorBId);
        assertThat(bakeryResults.get(0).sourceId()).isEqualTo(vendorCId);
    }

    @Test
    void reEmbedTriggersOnVendorScoreUpdatedEvent() throws Exception {
        chunkRepository.deleteBySourceTypeAndSourceId(SourceType.VENDOR_CAPABILITY, SEEDED_VENDOR_ID);
        assertThat(chunkRepository.findBySourceTypeAndSourceId(SourceType.VENDOR_CAPABILITY, SEEDED_VENDOR_ID)).isEmpty();

        VendorScoreUpdated event = VendorScoreUpdated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setVendorId(SEEDED_VENDOR_ID)
                .setSkuId(null)
                .setPreviousScore(0.8)
                .setNewScore(0.85)
                .setReason("integration test re-embed trigger")
                .build();
        produce("vendor-events", SEEDED_VENDOR_ID, event);

        Instant deadline = Instant.now().plusSeconds(30);
        while (Instant.now().isBefore(deadline)
                && chunkRepository.findBySourceTypeAndSourceId(SourceType.VENDOR_CAPABILITY, SEEDED_VENDOR_ID).isEmpty()) {
            Thread.sleep(1000);
        }

        assertThat(chunkRepository.findBySourceTypeAndSourceId(SourceType.VENDOR_CAPABILITY, SEEDED_VENDOR_ID)).isNotEmpty();
    }

    @Test
    void ingestAllViaRestEndpointPopulatesAtLeastTenVendorChunks() {
        ResponseEntity<IngestionService.IngestionStats> ingestResponse =
                restTemplate.postForEntity("/rag/ingest", null, IngestionService.IngestionStats.class);
        assertThat(ingestResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<RagController.RagStatusResponse> statusResponse =
                restTemplate.getForEntity("/rag/status", RagController.RagStatusResponse.class);
        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResponse.getBody().vendorCapabilityChunks()).isGreaterThanOrEqualTo(10);
    }

    private void produce(String topic, String key, org.apache.avro.specific.SpecificRecordBase event) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName());
        try (KafkaProducer<String, Object> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, key, event)).get(30, TimeUnit.SECONDS);
        }
    }
}
