package com.smartgrid.contractservice;

import com.smartgrid.commons.avro.order.OrderFulfilled;
import com.smartgrid.commons.avro.shipment.ShipmentDelivered;
import com.smartgrid.commons.avro.sla.ContractExpiring;
import com.smartgrid.commons.avro.sla.SLABreached;
import com.smartgrid.contractservice.domain.VendorDeliveryRecord;
import com.smartgrid.contractservice.dto.ContractResponse;
import com.smartgrid.contractservice.dto.CreateContractRequest;
import com.smartgrid.contractservice.dto.SlaTermRequest;
import com.smartgrid.contractservice.repository.VendorDeliveryRecordRepository;
import com.smartgrid.contractservice.search.ContractDocument;
import com.smartgrid.contractservice.service.ContractScheduledEvaluator;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContractServiceTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:18081";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VendorDeliveryRecordRepository deliveryRecordRepository;

    @Autowired
    private ContractScheduledEvaluator scheduledEvaluator;

    @Test
    void contractSearchReturnsResultsFromElasticsearch() throws InterruptedException {
        String uniqueTerm = "NetSuite90DayPayment" + UUID.randomUUID().toString().substring(0, 8);
        createContract("vendor-search-" + UUID.randomUUID(), uniqueTerm, 5, 100.0);

        List<ContractDocument> results = List.of();
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            ContractDocument[] found = restTemplate.getForObject("/contracts/search?q=" + uniqueTerm, ContractDocument[].class);
            results = found == null ? List.of() : List.of(found);
            if (!results.isEmpty()) {
                break;
            }
            Thread.sleep(500);
        }

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getTerms()).contains(uniqueTerm);
    }

    @Test
    void scheduledEvaluatorDetectsOverdueDeliveryAndPublishesSlaBreached() {
        String vendorId = "vendor-overdue-" + UUID.randomUUID();
        createContract(vendorId, "standard shipping terms", 1.0, 200.0);

        String orderId = "order-" + UUID.randomUUID();
        deliveryRecordRepository.save(new VendorDeliveryRecord(orderId, vendorId, Instant.now().minus(3, ChronoUnit.DAYS)));

        scheduledEvaluator.evaluate();

        SLABreached breach = awaitEvent("sla-events", SLABreached.class,
                e -> e.getVendorId().equals(vendorId), Duration.ofSeconds(15));
        assertThat(breach.getSeverity()).isNotBlank();
        assertThat(breach.getPenaltyAmount()).isGreaterThan(0);
    }

    @Test
    void eventDrivenDeliveryTriggersSlaBreachOnLateDelivery() throws Exception {
        String vendorId = "vendor-late-" + UUID.randomUUID();
        // ~2 seconds expressed as a fraction of a day, so a short real-time delay between the two events is
        // enough to exceed the threshold without waiting out an actual multi-day SLA window.
        createContract(vendorId, "fast lane terms", 2.0 / 86400.0, 50.0);

        String orderId = "order-" + UUID.randomUUID();
        produceOrderFulfilled(orderId, vendorId);
        Thread.sleep(3000);
        produceShipmentDelivered(orderId);

        SLABreached breach = awaitEvent("sla-events", SLABreached.class,
                e -> e.getVendorId().equals(vendorId), Duration.ofSeconds(20));
        assertThat(breach).isNotNull();
    }

    @Test
    void contractExpiringFiresWithinSevenDayWindow() {
        String vendorId = "vendor-expiring-" + UUID.randomUUID();
        ResponseEntity<ContractResponse> response = restTemplate.postForEntity("/contracts",
                new CreateContractRequest(vendorId, "expiring soon", LocalDate.now().minusDays(300), LocalDate.now().plusDays(5),
                        List.of(new SlaTermRequest("max_lead_time_days", 10.0, 100.0))),
                ContractResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        scheduledEvaluator.evaluate();

        ContractExpiring expiring = awaitEvent("sla-events", ContractExpiring.class,
                e -> e.getVendorId().equals(vendorId), Duration.ofSeconds(15));
        assertThat(expiring.getDaysRemaining()).isEqualTo(5);
    }

    private void createContract(String vendorId, String terms, double leadTimeThresholdDays, double penaltyPerBreach) {
        ResponseEntity<ContractResponse> response = restTemplate.postForEntity("/contracts",
                new CreateContractRequest(vendorId, terms, LocalDate.now().minusDays(30), LocalDate.now().plusDays(300),
                        List.of(new SlaTermRequest("max_lead_time_days", leadTimeThresholdDays, penaltyPerBreach))),
                ContractResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void produceOrderFulfilled(String orderId, String vendorId) throws Exception {
        OrderFulfilled event = OrderFulfilled.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setOrderId(orderId)
                .setVendorId(vendorId)
                .setQuoteId(UUID.randomUUID().toString())
                .setDestinationRegion("us-east")
                .build();
        produce("order-events", orderId, event);
    }

    private void produceShipmentDelivered(String orderId) throws Exception {
        ShipmentDelivered event = ShipmentDelivered.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setShipmentId(UUID.randomUUID().toString())
                .setOrderId(orderId)
                .build();
        produce("shipment-events", orderId, event);
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

    private <T> T awaitEvent(String topic, Class<T> type, Predicate<T> predicate, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "contract-service-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(2));
                for (ConsumerRecord<String, Object> record : records) {
                    if (type.isInstance(record.value())) {
                        T candidate = type.cast(record.value());
                        if (predicate.test(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        throw new AssertionError("Timed out waiting for matching " + type.getSimpleName() + " on topic " + topic);
    }
}
