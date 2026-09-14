package com.smartgrid.vendorservice;

import com.smartgrid.commons.avro.sla.SLABreached;
import com.smartgrid.vendorservice.dto.CreateVendorRequest;
import com.smartgrid.vendorservice.dto.UpdateRatingRequest;
import com.smartgrid.vendorservice.dto.VendorRankingResult;
import com.smartgrid.vendorservice.dto.VendorResponse;
import com.smartgrid.vendorservice.dto.VendorSkuRequest;
import com.smartgrid.vendorservice.search.VendorDocument;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VendorServiceTest {

    private static final String SCHEMA_REGISTRY_URL = "http://localhost:18081";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void topKReturnsVendorsInCorrectOrder() {
        String sku = "sku-" + UUID.randomUUID();
        createVendor("Cheap-Fast", "us-east", 100.0, 2, sku);
        createVendor("Expensive-Slow", "us-east", 500.0, 10, sku);
        createVendor("Mid", "us-east", 300.0, 6, sku);

        List<VendorRankingResult> top = topK(sku, 2);

        assertThat(top).hasSize(2);
        assertThat(top.get(0).compositeScore()).isGreaterThanOrEqualTo(top.get(1).compositeScore());
        assertThat(top.get(0).vendorName()).isEqualTo("Cheap-Fast");
    }

    @Test
    void secondRequestForSameSkuIsServedFromCache() {
        String sku = "sku-" + UUID.randomUUID();
        createVendor("Vendor-A", "eu-west", 150.0, 4, sku);

        List<VendorRankingResult> first = topK(sku, 5);
        boolean cacheKeyExistsAfterFirstCall = !redisTemplate.keys("vendor:top:v*:" + sku + ":5").isEmpty();
        List<VendorRankingResult> second = topK(sku, 5);

        assertThat(cacheKeyExistsAfterFirstCall).isTrue();
        assertThat(second).isEqualTo(first);
    }

    @Test
    void slaBreachedInvalidatesCacheAndDowngradesReliability() throws Exception {
        String sku = "sku-" + UUID.randomUUID();
        VendorResponse vendor = createVendor("Vendor-SLA", "ap-south", 200.0, 5, sku);
        assertThat(vendor.reliabilityScore()).isEqualTo(1.0);

        topK(sku, 5);
        Long versionBefore = currentCacheVersion();

        produceSlaBreached(vendor.id().toString(), "HIGH");

        Instant deadline = Instant.now().plusSeconds(20);
        double reliabilityAfter = vendor.reliabilityScore();
        while (Instant.now().isBefore(deadline)) {
            reliabilityAfter = getVendor(vendor.id()).reliabilityScore();
            if (reliabilityAfter < 1.0) {
                break;
            }
            Thread.sleep(300);
        }

        assertThat(reliabilityAfter).isEqualTo(0.75);
        Long versionAfter = currentCacheVersion();
        assertThat(versionAfter).isGreaterThan(versionBefore == null ? 0 : versionBefore);
    }

    @Test
    void manualRatingUpdateChangesReliabilityScore() {
        VendorResponse vendor = createVendor("Vendor-Rating", "us-west", 250.0, 3, "sku-" + UUID.randomUUID());

        ResponseEntity<Void> response = restTemplate.exchange(
                "/vendors/" + vendor.id() + "/rating", org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(new UpdateRatingRequest(0.5)), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(getVendor(vendor.id()).reliabilityScore()).isEqualTo(0.5);
    }

    @Test
    void fullTextSearchFindsVendorByNameAndCapability() {
        String uniqueTerm = "Zephyr" + UUID.randomUUID().toString().substring(0, 8);
        createVendorWithCapabilities(uniqueTerm, "us-east", uniqueTerm + "-capability");

        Instant deadline = Instant.now().plusSeconds(15);
        List<VendorDocument> results = List.of();
        while (Instant.now().isBefore(deadline)) {
            results = restTemplate.exchange(
                    "/vendors/search?q=" + uniqueTerm, org.springframework.http.HttpMethod.GET, null,
                    new org.springframework.core.ParameterizedTypeReference<List<VendorDocument>>() {
                    }).getBody();
            if (results != null && !results.isEmpty()) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        assertThat(results).isNotNull();
        assertThat(results).anyMatch(doc -> doc.getName().equals(uniqueTerm));
    }

    private VendorResponse createVendor(String name, String region, double price, int leadTimeDays, String sku) {
        CreateVendorRequest request = new CreateVendorRequest(
                name, region, 12.9, 77.6, "general",
                List.of(new VendorSkuRequest(sku, price, leadTimeDays)));
        ResponseEntity<VendorResponse> response = restTemplate.postForEntity("/vendors", request, VendorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private VendorResponse createVendorWithCapabilities(String name, String region, String capabilities) {
        CreateVendorRequest request = new CreateVendorRequest(name, region, 12.9, 77.6, capabilities, List.of());
        ResponseEntity<VendorResponse> response = restTemplate.postForEntity("/vendors", request, VendorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private VendorResponse getVendor(UUID id) {
        return restTemplate.getForObject("/vendors/" + id, VendorResponse.class);
    }

    private List<VendorRankingResult> topK(String sku, int k) {
        VendorRankingResult[] results = restTemplate.getForObject("/vendors/top?sku=" + sku + "&k=" + k, VendorRankingResult[].class);
        return List.of(results);
    }

    private Long currentCacheVersion() {
        String value = redisTemplate.opsForValue().get("vendor:cache:version");
        return value == null ? null : Long.parseLong(value);
    }

    private void produceSlaBreached(String vendorId, String severity) throws Exception {
        SLABreached event = SLABreached.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setBreachId(UUID.randomUUID().toString())
                .setVendorId(vendorId)
                .setContractId(UUID.randomUUID().toString())
                .setSeverity(severity)
                .setPenaltyAmount(1000.0)
                .build();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName());

        try (KafkaProducer<String, Object> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>("sla-events", vendorId, event)).get(30, TimeUnit.SECONDS);
        }
    }
}
