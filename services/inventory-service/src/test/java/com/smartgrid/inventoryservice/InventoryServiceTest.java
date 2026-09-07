package com.smartgrid.inventoryservice;

import com.smartgrid.commons.avro.inventory.StockDepleted;
import com.smartgrid.commons.avro.order.OrderCancelled;
import com.smartgrid.commons.avro.order.OrderCreated;
import com.smartgrid.commons.avro.order.OrderLineItem;
import com.smartgrid.inventoryservice.dto.AvailabilityResponse;
import com.smartgrid.inventoryservice.dto.ReleaseRequest;
import com.smartgrid.inventoryservice.dto.ReplenishRequest;
import com.smartgrid.inventoryservice.dto.ReserveRequest;
import com.smartgrid.inventoryservice.service.InventoryService;
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
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryServiceTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:18081";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InventoryService inventoryService;

    @Test
    void replenishReserveRelease_updatesAvailabilityCorrectly() {
        String sku = "sku-" + UUID.randomUUID();
        String warehouse = "wh-" + UUID.randomUUID();

        replenish(sku, warehouse, 10);
        assertThat(available(sku)).isEqualTo(10);

        reserve("order-1", sku, warehouse, 4);
        assertThat(available(sku)).isEqualTo(6);

        release("order-1", sku, warehouse, 2);
        assertThat(available(sku)).isEqualTo(8);
    }

    @Test
    void snapshotMatchesEventLogReplay() {
        String sku = "sku-" + UUID.randomUUID();
        String warehouse = "wh-" + UUID.randomUUID();

        replenish(sku, warehouse, 20);
        reserve("order-2", sku, warehouse, 7);
        reserve("order-3", sku, warehouse, 3);
        release("order-2", sku, warehouse, 1);

        long liveAvailable = available(sku);
        long replayed = inventoryService.replayAvailableQuantity(sku, warehouse);

        assertThat(liveAvailable).isEqualTo(replayed);
        assertThat(liveAvailable).isEqualTo(11);
    }

    @Test
    void concurrentReservationForLastUnit_exactlyOneSucceeds() throws Exception {
        String sku = "sku-" + UUID.randomUUID();
        String warehouse = "wh-" + UUID.randomUUID();
        replenish(sku, warehouse, 1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Future<org.springframework.http.HttpStatusCode> result1 = executor.submit(() -> reserveAfterLatch("order-a", sku, warehouse, 1, latch));
        Future<org.springframework.http.HttpStatusCode> result2 = executor.submit(() -> reserveAfterLatch("order-b", sku, warehouse, 1, latch));
        latch.countDown();

        org.springframework.http.HttpStatusCode status1 = result1.get();
        org.springframework.http.HttpStatusCode status2 = result2.get();
        executor.shutdown();

        assertThat(List.of(status1, status2)).containsExactlyInAnyOrder(HttpStatus.NO_CONTENT, HttpStatus.CONFLICT);
        assertThat(available(sku)).isEqualTo(0);
    }

    @Test
    void stockDepletedPublishedWhenReservationReachesZero() {
        String sku = "sku-" + UUID.randomUUID();
        String warehouse = "wh-" + UUID.randomUUID();
        replenish(sku, warehouse, 5);
        reserve("order-4", sku, warehouse, 5);

        StockDepleted event = awaitEvent("inventory-events", StockDepleted.class,
                e -> e.getSkuId().equals(sku), Duration.ofSeconds(20));

        assertThat(event.getWarehouseId()).isEqualTo(warehouse);
    }

    @Test
    void orderCreatedReservesStock_orderCancelledReleasesIt() throws Exception {
        String sku = "sku-" + UUID.randomUUID();
        String warehouse = "wh-" + UUID.randomUUID();
        replenish(sku, warehouse, 10);

        String orderId = UUID.randomUUID().toString();
        produce("order-events", orderId, OrderCreated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setOrderId(orderId)
                .setRequestedBy("planner")
                .setDestinationRegion("us-east")
                .setItems(List.of(OrderLineItem.newBuilder().setSkuId(sku).setQuantity(4).build()))
                .build());

        Instant deadline = Instant.now().plusSeconds(20);
        long afterReservation = available(sku);
        while (Instant.now().isBefore(deadline) && afterReservation == 10) {
            Thread.sleep(300);
            afterReservation = available(sku);
        }
        assertThat(afterReservation).isEqualTo(6);

        produce("order-events", orderId, OrderCancelled.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setOrderId(orderId)
                .setReason("test cancellation")
                .build());

        deadline = Instant.now().plusSeconds(20);
        long afterRelease = afterReservation;
        while (Instant.now().isBefore(deadline) && afterRelease != 10) {
            Thread.sleep(300);
            afterRelease = available(sku);
        }
        assertThat(afterRelease).isEqualTo(10);
    }

    private org.springframework.http.HttpStatusCode reserveAfterLatch(String orderId, String sku, String warehouse, long quantity, CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/inventory/reserve", new ReserveRequest(orderId, sku, warehouse, quantity), String.class);
        return response.getStatusCode();
    }

    private void replenish(String sku, String warehouse, long quantity) {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/inventory/replenish", new ReplenishRequest(sku, warehouse, quantity), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private void reserve(String orderId, String sku, String warehouse, long quantity) {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/inventory/reserve", new ReserveRequest(orderId, sku, warehouse, quantity), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private void release(String orderId, String sku, String warehouse, long quantity) {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/inventory/release", new ReleaseRequest(orderId, sku, warehouse, quantity), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private long available(String sku) {
        AvailabilityResponse response = restTemplate.getForObject("/inventory/" + sku + "/available", AvailabilityResponse.class);
        return response.availableQuantity();
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
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-service-test-" + UUID.randomUUID());
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
