package com.smartgrid.analyticsservice.streams;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

import java.util.Map;

/**
 * The same KafkaAvroSerializer/Deserializer every other service's producer/consumer factory uses,
 * wrapped as a Kafka Streams Serde<Object> rather than the type-parameterized SpecificAvroSerde<T> —
 * source topics here carry multiple record types via TopicRecordNameStrategy, the same reason every
 * @KafkaListener elsewhere in this codebase deserializes to Object and instanceof-checks the result.
 */
public final class GenericAvroSerdeFactory {

    private GenericAvroSerdeFactory() {
    }

    public static Serde<Object> create(String schemaRegistryUrl) {
        Map<String, Object> config = Map.of(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        KafkaAvroSerializer serializer = new KafkaAvroSerializer();
        serializer.configure(config, false);
        KafkaAvroDeserializer deserializer = new KafkaAvroDeserializer();
        deserializer.configure(config, false);

        return Serdes.serdeFrom(serializer, deserializer);
    }
}
