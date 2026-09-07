package com.smartgrid.analyticsservice.streams;

import com.smartgrid.analyticsservice.config.AnalyticsProperties;
import com.smartgrid.commons.avro.analytics.AnalyticsAggregateComputed;
import com.smartgrid.commons.avro.disruption.DisruptionDetected;
import com.smartgrid.commons.avro.order.OrderCreated;
import com.smartgrid.commons.avro.order.OrderFulfilled;
import com.smartgrid.commons.avro.reroute.EscalationRequired;
import com.smartgrid.commons.avro.reroute.RerouteDecision;
import com.smartgrid.commons.avro.shipment.ShipmentDelivered;
import com.smartgrid.commons.avro.sla.SLABreached;
import com.smartgrid.commons.avro.vendor.VendorScoreUpdated;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Configuration
public class AnalyticsTopology {

    public static final String OUTPUT_TOPIC = "analytics-aggregates";

    private final StreamsBuilder streamsBuilder;
    private final AnalyticsProperties properties;
    private final Serde<Object> avroSerde;

    public AnalyticsTopology(StreamsBuilder streamsBuilder, AnalyticsProperties properties,
            @Value("${smartgrid.kafka.schema-registry-url}") String schemaRegistryUrl) {
        this.streamsBuilder = streamsBuilder;
        this.properties = properties;
        this.avroSerde = GenericAvroSerdeFactory.create(schemaRegistryUrl);
        buildTopology();
    }

    private void buildTopology() {
        TimeWindows tumbling = TimeWindows.ofSizeWithNoGrace(Duration.ofMillis(properties.getTumblingWindowMs()));
        TimeWindows hopping = TimeWindows.ofSizeWithNoGrace(Duration.ofMillis(properties.getHoppingWindowMs()))
                .advanceBy(Duration.ofMillis(properties.getHoppingAdvanceMs()));

        buildDisruptionCount(tumbling, hopping);
        buildOrderPipelineCounts(tumbling);
        buildRerouteOutcomeCounts(tumbling);
        buildSlaBreachCount(hopping);
        buildVendorScoreLatest(tumbling);
        buildShipmentDeliveredCount(tumbling);
    }

    private void buildDisruptionCount(TimeWindows tumbling, TimeWindows hopping) {
        KStream<String, Object> source = streamsBuilder.stream("disruption-detected", Consumed.with(Serdes.String(), avroSerde));
        KStream<String, Object> disruptions = source.filter((k, v) -> v instanceof DisruptionDetected);

        KStream<String, Object> byRegion = disruptions.groupBy(
                (k, v) -> ((DisruptionDetected) v).getRegion(), Grouped.with(Serdes.String(), avroSerde))
                .windowedBy(tumbling)
                .count(Materialized.as("disruption-count-tumbling-store"))
                .toStream()
                .map((windowedKey, count) -> toAggregateKeyValue(
                        "disruption_count", "TUMBLING", windowedKey, count.doubleValue(),
                        Map.of("region", windowedKey.key())));
        byRegion.to(OUTPUT_TOPIC, Produced.with(Serdes.String(), avroSerde));

        disruptions.groupBy((k, v) -> ((DisruptionDetected) v).getRegion(), Grouped.with(Serdes.String(), avroSerde))
                .windowedBy(hopping)
                .count(Materialized.as("disruption-count-hopping-store"))
                .toStream()
                .map((windowedKey, count) -> toAggregateKeyValue(
                        "disruption_count", "HOPPING", windowedKey, count.doubleValue(),
                        Map.of("region", windowedKey.key())))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), avroSerde));
    }

    private void buildOrderPipelineCounts(TimeWindows tumbling) {
        KStream<String, Object> source = streamsBuilder.stream("order-events", Consumed.with(Serdes.String(), avroSerde));

        source.filter((k, v) -> v instanceof OrderCreated)
                .groupBy((k, v) -> {
                    String region = ((OrderCreated) v).getDestinationRegion();
                    return region == null ? "unknown" : region;
                }, Grouped.with(Serdes.String(), avroSerde))
                .windowedBy(tumbling)
                .count(Materialized.as("order-created-tumbling-store"))
                .toStream()
                .map((windowedKey, count) -> toAggregateKeyValue(
                        "order_created_count", "TUMBLING", windowedKey, count.doubleValue(),
                        Map.of("region", windowedKey.key())))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), avroSerde));

        source.filter((k, v) -> v instanceof OrderFulfilled)
                .groupBy((k, v) -> {
                    String region = ((OrderFulfilled) v).getDestinationRegion();
                    return region == null ? "unknown" : region;
                }, Grouped.with(Serdes.String(), avroSerde))
                .windowedBy(tumbling)
                .count(Materialized.as("order-throughput-tumbling-store"))
                .toStream()
                .map((windowedKey, count) -> toAggregateKeyValue(
                        "order_throughput", "TUMBLING", windowedKey, count.doubleValue(),
                        Map.of("region", windowedKey.key())))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), avroSerde));
    }

    private void buildShipmentDeliveredCount(TimeWindows tumbling) {
        KStream<String, Object> source = streamsBuilder.stream("shipment-events", Consumed.with(Serdes.String(), avroSerde));

        source.filter((k, v) -> v instanceof ShipmentDelivered)
                .groupBy((k, v) -> "all", Grouped.with(Serdes.String(), avroSerde))
                .windowedBy(tumbling)
                .count(Materialized.as("shipment-delivered-tumbling-store"))
                .toStream()
                .map((windowedKey, count) -> toAggregateKeyValue(
                        "shipment_delivered_count", "TUMBLING", windowedKey, count.doubleValue(), Map.of()))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), avroSerde));
    }

    private void buildRerouteOutcomeCounts(TimeWindows tumbling) {
        KStream<String, Object> source = streamsBuilder.stream("reroute-decisions", Consumed.with(Serdes.String(), avroSerde));

        source.filter((k, v) -> v instanceof RerouteDecision)
                .groupBy((k, v) -> "all", Grouped.with(Serdes.String(), avroSerde))
                .windowedBy(tumbling)
                .count(Materialized.as("reroute-success-tumbling-store"))
                .toStream()
                .map((windowedKey, count) -> toAggregateKeyValue(
                        "reroute_success_count", "TUMBLING", windowedKey, count.doubleValue(), Map.of()))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), avroSerde));

        source.filter((k, v) -> v instanceof EscalationRequired)
                .groupBy((k, v) -> "all", Grouped.with(Serdes.String(), avroSerde))
                .windowedBy(tumbling)
                .count(Materialized.as("reroute-escalation-tumbling-store"))
                .toStream()
                .map((windowedKey, count) -> toAggregateKeyValue(
                        "reroute_escalation_count", "TUMBLING", windowedKey, count.doubleValue(), Map.of()))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), avroSerde));
    }

    private void buildSlaBreachCount(TimeWindows hopping) {
        KStream<String, Object> source = streamsBuilder.stream("sla-events", Consumed.with(Serdes.String(), avroSerde));

        source.filter((k, v) -> v instanceof SLABreached)
                .groupBy((k, v) -> ((SLABreached) v).getVendorId(), Grouped.with(Serdes.String(), avroSerde))
                .windowedBy(hopping)
                .count(Materialized.as("sla-breach-hopping-store"))
                .toStream()
                .map((windowedKey, count) -> toAggregateKeyValue(
                        "sla_breach_count", "HOPPING", windowedKey, count.doubleValue(),
                        Map.of("vendorId", windowedKey.key())))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), avroSerde));
    }

    private void buildVendorScoreLatest(TimeWindows tumbling) {
        KStream<String, Object> source = streamsBuilder.stream("vendor-events", Consumed.with(Serdes.String(), avroSerde));

        KTable<Windowed<String>, Double> latestScore = source.filter((k, v) -> v instanceof VendorScoreUpdated)
                .groupBy((k, v) -> ((VendorScoreUpdated) v).getVendorId(), Grouped.with(Serdes.String(), avroSerde))
                .windowedBy(tumbling)
                .aggregate(() -> 0.0, (key, newValue, aggregate) -> ((VendorScoreUpdated) newValue).getNewScore(),
                        Materialized.with(Serdes.String(), Serdes.Double()));

        latestScore.toStream()
                .map((windowedKey, score) -> toAggregateKeyValue(
                        "vendor_score", "TUMBLING", windowedKey, score, Map.of("vendorId", windowedKey.key())))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), avroSerde));
    }

    private KeyValue<String, Object> toAggregateKeyValue(String metricType, String windowType,
            Windowed<String> windowedKey, double value, Map<String, String> dimensions) {
        AnalyticsAggregateComputed event = AnalyticsAggregateComputed.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(UUID.randomUUID().toString())
                .setMetricType(metricType)
                .setWindowType(windowType)
                .setWindowStart(windowedKey.window().start())
                .setWindowEnd(windowedKey.window().end())
                .setValue(value)
                .setDimensions(dimensions)
                .build();
        return KeyValue.pair(metricType + ":" + windowedKey.key(), event);
    }
}
