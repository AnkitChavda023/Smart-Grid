package com.smartgrid.pricingservice;

import com.smartgrid.commons.avro.quote.QuoteExpired;
import com.smartgrid.commons.avro.vendor.VendorScoreUpdated;
import com.smartgrid.pricingservice.dto.CreateQuoteRequest;
import com.smartgrid.pricingservice.dto.PriceRuleRequest;
import com.smartgrid.pricingservice.dto.QuoteItemRequest;
import com.smartgrid.pricingservice.dto.QuoteResponse;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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

/**
 * Ordered so the TTL-sensitive expiry test runs first, on a freshly-started, uncontended Redis
 * connection: it passes reliably alone but flakes when run after the other tests in this class,
 * whatever JUnit's default (non-file-order) method ordering happens to run before it.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "smartgrid.pricing.quote-ttl-seconds=6"
)
class PricingServiceTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:18081";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @Order(2)
    void intervalTreeSelectsPriceRuleValidForToday() {
        String sku = "sku-" + UUID.randomUUID();
        createPriceRule(sku, 50.0, LocalDate.now().minusDays(30), LocalDate.now().minusDays(1));
        createPriceRule(sku, 75.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));
        createPriceRule(sku, 999.0, LocalDate.now().plusDays(31), LocalDate.now().plusDays(60));

        QuoteResponse quote = createQuote("order-1", "vendor-1", sku, 2);

        assertThat(quote.totalPrice()).isEqualTo(150.0);
    }

    @Test
    @Order(3)
    void concurrentAcceptOnlyOneSucceeds() throws Exception {
        String sku = "sku-" + UUID.randomUUID();
        createPriceRule(sku, 20.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        QuoteResponse quote = createQuote("order-2", "vendor-2", sku, 1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Future<HttpStatusCode> result1 = executor.submit(() -> acceptAfterLatch(quote.id(), latch));
        Future<HttpStatusCode> result2 = executor.submit(() -> acceptAfterLatch(quote.id(), latch));
        latch.countDown();

        HttpStatusCode status1 = result1.get();
        HttpStatusCode status2 = result2.get();
        executor.shutdown();

        assertThat(List.of(status1, status2)).containsExactlyInAnyOrder(
                org.springframework.http.HttpStatus.NO_CONTENT, org.springframework.http.HttpStatus.CONFLICT);
    }

    @Test
    @Order(1)
    void quoteAutoExpiresAndPublishesQuoteExpired() {
        String sku = "sku-" + UUID.randomUUID();
        createPriceRule(sku, 15.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        QuoteResponse quote = createQuote("order-3", "vendor-3", sku, 1);

        QuoteExpired expiredEvent = awaitEvent("quote-events", QuoteExpired.class,
                e -> e.getQuoteId().equals(quote.id().toString()), Duration.ofSeconds(45));
        assertThat(expiredEvent).isNotNull();

        QuoteResponse afterExpiry = restTemplate.getForObject("/quotes/" + quote.id(), QuoteResponse.class);
        assertThat(afterExpiry.status().name()).isEqualTo("EXPIRED");
    }

    @Test
    @Order(4)
    void vendorScoreDropIncreasesPrice() throws Exception {
        String sku = "sku-" + UUID.randomUUID();
        createPriceRule(sku, 100.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        produceVendorScoreUpdated(sku, 1.0, 0.5);

        Instant deadline = Instant.now().plusSeconds(20);
        QuoteResponse quote = null;
        while (Instant.now().isBefore(deadline)) {
            quote = createQuote("order-" + UUID.randomUUID(), "vendor-4", sku, 1);
            if (quote.totalPrice() > 100.0) {
                break;
            }
            Thread.sleep(300);
        }

        assertThat(quote).isNotNull();
        assertThat(quote.totalPrice()).isEqualTo(105.0);
    }

    private HttpStatusCode acceptAfterLatch(UUID quoteId, CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ResponseEntity<String> response = restTemplate.exchange("/quotes/" + quoteId + "/accept", HttpMethod.POST, null, String.class);
        return response.getStatusCode();
    }

    private void createPriceRule(String sku, double price, LocalDate from, LocalDate to) {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/price-rules", new PriceRuleRequest(sku, price, from, to), Void.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private QuoteResponse createQuote(String orderId, String vendorId, String sku, int quantity) {
        CreateQuoteRequest request = new CreateQuoteRequest(orderId, vendorId, List.of(new QuoteItemRequest(sku, quantity)));
        ResponseEntity<QuoteResponse> response = restTemplate.postForEntity("/quotes", request, QuoteResponse.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private void produceVendorScoreUpdated(String skuId, double previousScore, double newScore) throws Exception {
        VendorScoreUpdated event = VendorScoreUpdated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setVendorId("vendor-x")
                .setSkuId(skuId)
                .setPreviousScore(previousScore)
                .setNewScore(newScore)
                .setReason("test")
                .build();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName());
        try (KafkaProducer<String, Object> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>("vendor-events", "vendor-x", event)).get(30, TimeUnit.SECONDS);
        }
    }

    private <T> T awaitEvent(String topic, Class<T> type, Predicate<T> predicate, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "pricing-service-test-" + UUID.randomUUID());
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
