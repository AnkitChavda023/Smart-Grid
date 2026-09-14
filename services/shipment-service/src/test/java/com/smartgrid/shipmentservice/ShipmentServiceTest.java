package com.smartgrid.shipmentservice;

import com.smartgrid.commons.avro.order.OrderFulfilled;
import com.smartgrid.commons.avro.shipment.ShipmentDelayed;
import com.smartgrid.commons.avro.shipment.ShipmentDelivered;
import com.smartgrid.shipmentservice.dto.CheckpointRequest;
import com.smartgrid.shipmentservice.dto.LiveLocationMessage;
import com.smartgrid.shipmentservice.dto.ShipmentResponse;
import com.smartgrid.shipmentservice.dto.WarehouseRequest;
import com.smartgrid.shipmentservice.dto.WarehouseResponse;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShipmentServiceTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:18081";

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void nearestWarehouseQueryReturnsCorrectResult() {
        // Randomized per run so leftover warehouses from earlier test executions (this table is never cleaned up)
        // can never sit at the exact same coordinates and create a nearest-neighbor tie.
        double baseLat = ThreadLocalRandom.current().nextDouble(-60, 60);
        double baseLon = ThreadLocalRandom.current().nextDouble(-150, 150);

        createWarehouse("far-warehouse-" + UUID.randomUUID(), baseLat + 20.0, baseLon + 20.0);
        WarehouseResponse near = createWarehouse("near-warehouse-" + UUID.randomUUID(), baseLat, baseLon);
        createWarehouse("mid-warehouse-" + UUID.randomUUID(), baseLat + 5.0, baseLon + 5.0);

        List<WarehouseResponse> nearest = List.of(restTemplate.getForObject(
                "/warehouses/nearest?lat=" + baseLat + "&lon=" + baseLon + "&limit=1", WarehouseResponse[].class));

        assertThat(nearest).hasSize(1);
        assertThat(nearest.get(0).id()).isEqualTo(near.id());
    }

    @Test
    void checkpointUpdatesEtaAndPushesLiveLocationOverWebSocket() throws Exception {
        createWarehouse("origin-wh-" + UUID.randomUUID(), 12.9, 77.6);
        String orderId = UUID.randomUUID().toString();
        produceOrderFulfilled(orderId, "vendor-1", "us-east");

        ShipmentResponse shipment = awaitShipmentForOrder(orderId, Duration.ofSeconds(20));

        BlockingQueue<LiveLocationMessage> messages = subscribeToLiveLocation(shipment.id());

        recordCheckpoint(shipment.id(), 12.91, 77.61);

        LiveLocationMessage received = messages.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.latitude()).isEqualTo(12.91);

        ShipmentResponse afterCheckpoint = restTemplate.getForObject("/shipments/" + shipment.id(), ShipmentResponse.class);
        assertThat(afterCheckpoint.checkpointCount()).isEqualTo(1);
    }

    @Test
    void shipmentDelayedFiresWhenGapExceedsExpectedPace() throws Exception {
        createWarehouse("origin-wh-" + UUID.randomUUID(), 12.9, 77.6);
        String orderId = UUID.randomUUID().toString();
        produceOrderFulfilled(orderId, "vendor-2", "eu-west");
        ShipmentResponse shipment = awaitShipmentForOrder(orderId, Duration.ofSeconds(20));

        recordCheckpoint(shipment.id(), 10.0, 10.0);
        Thread.sleep(500);
        recordCheckpoint(shipment.id(), 10.01, 10.01);
        Thread.sleep(500);
        recordCheckpoint(shipment.id(), 10.02, 10.02);

        Thread.sleep(3000);
        recordCheckpoint(shipment.id(), 10.03, 10.03);

        ShipmentDelayed delayed = awaitEvent("shipment-events", ShipmentDelayed.class,
                e -> e.getShipmentId().equals(shipment.id().toString()), Duration.ofSeconds(15));
        assertThat(delayed).isNotNull();

        ShipmentResponse afterDelay = restTemplate.getForObject("/shipments/" + shipment.id(), ShipmentResponse.class);
        assertThat(afterDelay.status().name()).isEqualTo("DELAYED");
    }

    @Test
    void markDeliveredPublishesShipmentDelivered() throws Exception {
        createWarehouse("origin-wh-" + UUID.randomUUID(), 12.9, 77.6);
        String orderId = UUID.randomUUID().toString();
        produceOrderFulfilled(orderId, "vendor-3", "ap-south");
        ShipmentResponse shipment = awaitShipmentForOrder(orderId, Duration.ofSeconds(20));

        ResponseEntity<Void> response = restTemplate.postForEntity("/shipments/" + shipment.id() + "/deliver", null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ShipmentDelivered delivered = awaitEvent("shipment-events", ShipmentDelivered.class,
                e -> e.getShipmentId().equals(shipment.id().toString()), Duration.ofSeconds(15));
        assertThat(delivered).isNotNull();
    }

    private WarehouseResponse createWarehouse(String name, double lat, double lon) {
        ResponseEntity<WarehouseResponse> response = restTemplate.postForEntity(
                "/warehouses", new WarehouseRequest(name, lat, lon), WarehouseResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private void recordCheckpoint(UUID shipmentId, double lat, double lon) {
        ResponseEntity<ShipmentResponse> response = restTemplate.exchange(
                "/shipments/" + shipmentId + "/checkpoint", HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(new CheckpointRequest(lat, lon)), ShipmentResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ShipmentResponse awaitShipmentForOrder(String orderId, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try {
                ResponseEntity<ShipmentResponse[]> response = restTemplate.getForEntity("/shipments?orderId=" + orderId, ShipmentResponse[].class);
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && response.getBody().length > 0) {
                    return response.getBody()[0];
                }
            } catch (Exception notReadyYet) {
                // Keep polling until the shipment appears
            }
            Thread.sleep(300);
        }
        throw new AssertionError("Shipment for order " + orderId + " never appeared");
    }

    private BlockingQueue<LiveLocationMessage> subscribeToLiveLocation(UUID shipmentId) throws Exception {
        BlockingQueue<LiveLocationMessage> queue = new LinkedBlockingQueue<>();
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.setTaskScheduler(new ConcurrentTaskScheduler());

        StompSession session = stompClient.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
        }).get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/shipments/" + shipmentId + "/live", new org.springframework.messaging.simp.stomp.StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return LiveLocationMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((LiveLocationMessage) payload);
            }
        });

        Thread.sleep(500);
        return queue;
    }

    private void produceOrderFulfilled(String orderId, String vendorId, String destinationRegion) throws Exception {
        OrderFulfilled event = OrderFulfilled.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setOrderId(orderId)
                .setVendorId(vendorId)
                .setQuoteId(UUID.randomUUID().toString())
                .setDestinationRegion(destinationRegion)
                .build();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName());
        try (KafkaProducer<String, Object> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>("order-events", orderId, event)).get(30, TimeUnit.SECONDS);
        }
    }

    private <T> T awaitEvent(String topic, Class<T> type, Predicate<T> predicate, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "shipment-service-test-" + UUID.randomUUID());
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
