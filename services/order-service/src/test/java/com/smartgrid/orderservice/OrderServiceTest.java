package com.smartgrid.orderservice;

import com.smartgrid.commons.avro.inventory.ReservationConfirmed;
import com.smartgrid.commons.avro.order.OrderCreated;
import com.smartgrid.commons.avro.order.OrderFulfilled;
import com.smartgrid.commons.avro.quote.QuoteAccepted;
import com.smartgrid.commons.avro.vendor.VendorConfirmed;
import com.smartgrid.orderservice.dto.CreateOrderRequest;
import com.smartgrid.orderservice.dto.OrderItemRequest;
import com.smartgrid.orderservice.dto.OrderResponse;
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
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:18081";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createOrder_publishesOutboxEventToOrderEventsTopic() {
        UUID orderId = createOrder("planner-1", "us-east", "sku-1", 3);

        OrderCreated event = awaitEvent("order-events", OrderCreated.class,
                e -> e.getOrderId().equals(orderId.toString()), Duration.ofSeconds(30));

        assertThat(event.getRequestedBy()).isEqualTo("planner-1");
        assertThat(event.getDestinationRegion()).isEqualTo("us-east");
        assertThat(event.getItems()).hasSize(1);
        assertThat(event.getItems().get(0).getSkuId()).isEqualTo("sku-1");
    }

    @Test
    void fullSagaHappyPath_confirmsOrderAndPublishesOrderFulfilled() {
        UUID orderId = createOrder("planner-2", "eu-west", "sku-2", 5);

        produce("inventory-events", orderId.toString(), ReservationConfirmed.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setOrderId(orderId.toString())
                .setSkuId("sku-2")
                .setQuantity(5)
                .setWarehouseId("warehouse-1")
                .build());

        produce("vendor-events", "vendor-99", VendorConfirmed.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setOrderId(orderId.toString())
                .setVendorId("vendor-99")
                .setSkuId("sku-2")
                .setQuantity(5)
                .build());

        produce("quote-events", orderId.toString(), QuoteAccepted.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setQuoteId("quote-77")
                .setOrderId(orderId.toString())
                .build());

        OrderResponse confirmed = awaitOrderStatus(orderId, "CONFIRMED", Duration.ofSeconds(60));
        assertThat(confirmed.vendorId()).isEqualTo("vendor-99");
        assertThat(confirmed.quoteId()).isEqualTo("quote-77");

        OrderFulfilled fulfilledEvent = awaitEvent("order-events", OrderFulfilled.class,
                e -> e.getOrderId().equals(orderId.toString()), Duration.ofSeconds(30));
        assertThat(fulfilledEvent.getVendorId()).isEqualTo("vendor-99");
        assertThat(fulfilledEvent.getQuoteId()).isEqualTo("quote-77");
    }

    @Test
    void cancel_transitionsToCancelledAndRejectsSecondCancel() {
        UUID orderId = createOrder("planner-3", "ap-south", "sku-3", 1);

        ResponseEntity<Void> first = restTemplate.exchange(
                "/orders/" + orderId + "/cancel", org.springframework.http.HttpMethod.PATCH, null, Void.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        OrderResponse cancelled = restTemplate.getForObject("/orders/" + orderId, OrderResponse.class);
        assertThat(cancelled.status().name()).isEqualTo("CANCELLED");

        ResponseEntity<String> second = restTemplate.exchange(
                "/orders/" + orderId + "/cancel", org.springframework.http.HttpMethod.PATCH, null, String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void concurrentCancel_onlyOneSucceeds() throws Exception {
        UUID orderId = createOrder("planner-4", "us-west", "sku-4", 2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<HttpStatus> result1 = executor.submit(() -> cancelAfterLatch(orderId, startLatch));
        Future<HttpStatus> result2 = executor.submit(() -> cancelAfterLatch(orderId, startLatch));
        startLatch.countDown();

        HttpStatus status1 = result1.get();
        HttpStatus status2 = result2.get();
        executor.shutdown();

        List<HttpStatus> statuses = List.of(status1, status2);
        assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.NO_CONTENT, HttpStatus.CONFLICT);
    }

    private HttpStatus cancelAfterLatch(UUID orderId, CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ResponseEntity<String> response = restTemplate.exchange(
                "/orders/" + orderId + "/cancel", org.springframework.http.HttpMethod.PATCH, null, String.class);
        return (HttpStatus) response.getStatusCode();
    }

    private UUID createOrder(String requestedBy, String region, String sku, int qty) {
        CreateOrderRequest request = new CreateOrderRequest(requestedBy, region, List.of(new OrderItemRequest(sku, qty)));
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity("/orders", request, OrderResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().id();
    }

    private OrderResponse awaitOrderStatus(UUID orderId, String expectedStatus, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            OrderResponse order = restTemplate.getForObject("/orders/" + orderId, OrderResponse.class);
            if (order.status().name().equals(expectedStatus)) {
                return order;
            }
            sleepQuietly();
        }
        throw new AssertionError("Order " + orderId + " did not reach status " + expectedStatus + " within " + timeout);
    }

    private void produce(String topic, String key, org.apache.avro.specific.SpecificRecordBase event) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName());
        try (KafkaProducer<String, Object> producer = new KafkaProducer<>(props)) {
            try {
                producer.send(new ProducerRecord<>(topic, key, event)).get(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private <T> T awaitEvent(String topic, Class<T> type, Predicate<T> predicate, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service-test-" + UUID.randomUUID());
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

    private void sleepQuietly() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
