package com.smartgrid.commons.kafka;

import com.smartgrid.commons.avro.order.OrderCreated;
import com.smartgrid.commons.avro.order.OrderLineItem;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Targets the real M01 Kafka + Schema Registry (docker compose up), not Testcontainers.
class AvroKafkaRoundTripIT {

    private static final String BOOTSTRAP_SERVERS =
            System.getenv().getOrDefault("SMARTGRID_KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    private static final String SCHEMA_REGISTRY_URL =
            System.getenv().getOrDefault("SMARTGRID_SCHEMA_REGISTRY_URL", "http://localhost:18081");

    private static String topic;

    @BeforeAll
    static void createTopic() throws Exception {
        topic = "smartgrid-commons-it-" + UUID.randomUUID();
        try (AdminClient admin = AdminClient.create(adminConfig())) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get(30, TimeUnit.SECONDS);
        }
    }

    @AfterAll
    static void deleteTopic() throws Exception {
        try (AdminClient admin = AdminClient.create(adminConfig())) {
            admin.deleteTopics(List.of(topic)).all().get(30, TimeUnit.SECONDS);
        }
    }

    @Test
    void producesAndConsumesOrderCreatedEvent() throws Exception {
        OrderCreated event = OrderCreated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setOrderId("order-123")
                .setRequestedBy("planner-1")
                .setDestinationRegion("us-east")
                .setItems(List.of(OrderLineItem.newBuilder().setSkuId("sku-1").setQuantity(2).build()))
                .build();

        try (KafkaProducer<String, Object> producer = new KafkaProducer<>(producerConfig())) {
            producer.send(new ProducerRecord<>(topic, event.getOrderId(), event)).get(30, TimeUnit.SECONDS);
        }

        try (KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(consumerConfig())) {
            consumer.subscribe(List.of(topic));
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(15));

            assertThat(records.count()).isEqualTo(1);
            ConsumerRecord<String, Object> record = records.iterator().next();
            OrderCreated consumed = (OrderCreated) record.value();

            assertThat(consumed.getOrderId()).isEqualTo("order-123");
            assertThat(consumed.getDestinationRegion()).isEqualTo("us-east");
            assertThat(consumed.getItems()).hasSize(1);
            assertThat(consumed.getItems().get(0).getSkuId()).isEqualTo("sku-1");
            assertThat(consumed.getItems().get(0).getQuantity()).isEqualTo(2);
        }
    }

    private static Properties adminConfig() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        return props;
    }

    private Properties producerConfig() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return props;
    }

    private Properties consumerConfig() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "smartgrid-commons-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}
