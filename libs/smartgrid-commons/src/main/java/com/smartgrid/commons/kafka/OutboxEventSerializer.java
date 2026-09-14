package com.smartgrid.commons.kafka;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.Map;

public class OutboxEventSerializer {

    private final KafkaAvroSerializer avroSerializer;

    public OutboxEventSerializer(SmartGridKafkaProperties properties) {
        this.avroSerializer = new KafkaAvroSerializer();
        this.avroSerializer.configure(Map.of(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, properties.getSchemaRegistryUrl(),
                KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, true,
                KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName()
        ), false);
    }

    public byte[] serialize(String topic, SpecificRecordBase record) {
        return avroSerializer.serialize(topic, record);
    }
}
