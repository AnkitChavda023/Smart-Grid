package com.smartgrid.rerouteagent;

import com.smartgrid.commons.avro.reroute.RerouteDecision;
import com.smartgrid.rerouteagent.domain.Reroute;
import com.smartgrid.rerouteagent.domain.RerouteStatus;
import com.smartgrid.rerouteagent.repository.RerouteRepository;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers persistence, the DAG-trace REST surface, and the human-approval path — none of which need
 * a real LLM call. The ReAct loop itself (tool-calling AiServices against the real MCP server) is
 * fully wired and compiles/boots against the real stack; the actual model completion is blocked on
 * the same exhausted OpenAI credits as M14/the Disruption Detector. See docs/modules/M16-core-agents.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RerouteAgentTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:18081";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RerouteRepository rerouteRepository;

    @Test
    void getRerouteForUnknownIdReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/reroutes/{id}", String.class, UUID.randomUUID());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void traceReturnsTheStoredDagNodesForAnEscalatedReroute() {
        String orderId = UUID.randomUUID().toString();
        Reroute escalated = new Reroute(UUID.randomUUID(), orderId, "disruption-1", null, null, 0.4,
                "[{\"stepIndex\":0,\"toolName\":\"searchVendors\",\"input\":\"{}\",\"output\":\"[]\",\"latencyMs\":12,\"llmReasoning\":\"ReAct step 1\"}]",
                RerouteStatus.ESCALATED);
        rerouteRepository.save(escalated);

        ResponseEntity<List> response = restTemplate.getForEntity("/reroutes/{id}/trace", List.class, escalated.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void approvingAnEscalatedRerouteTransitionsStatusAndPublishesRerouteDecision() throws Exception {
        String orderId = UUID.randomUUID().toString();
        Reroute escalated = new Reroute(UUID.randomUUID(), orderId, "disruption-2", null, null, 0.3, "[]", RerouteStatus.ESCALATED);
        rerouteRepository.save(escalated);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(
                "{\"selectedVendorId\":\"vendor-approved\",\"quoteId\":\"quote-approved\"}", headers);

        ResponseEntity<Reroute> response = restTemplate.postForEntity("/reroutes/{id}/approve", request, Reroute.class, escalated.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(RerouteStatus.APPROVED);

        RerouteDecision decision = awaitEvent("reroute-decisions", RerouteDecision.class,
                d -> d.getRerouteId().equals(escalated.getId().toString()), Duration.ofSeconds(20));
        assertThat(decision.getSelectedVendorId()).isEqualTo("vendor-approved");
        assertThat(decision.getQuoteId()).isEqualTo("quote-approved");
    }

    private <T> T awaitEvent(String topic, Class<T> type, Predicate<T> predicate, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "reroute-planner-agent-test-" + UUID.randomUUID());
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
