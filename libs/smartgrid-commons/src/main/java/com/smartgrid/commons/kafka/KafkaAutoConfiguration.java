package com.smartgrid.commons.kafka;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@AutoConfiguration(before = org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class)
@EnableConfigurationProperties(SmartGridKafkaProperties.class)
@ConditionalOnProperty(prefix = "smartgrid.kafka", name = "bootstrap-servers")
public class KafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, Object> smartGridProducerFactory(SmartGridKafkaProperties properties) {
        return SmartGridProducerFactory.create(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, Object> smartGridKafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumerFactory<String, Object> smartGridConsumerFactory(SmartGridKafkaProperties properties) {
        return SmartGridConsumerFactory.create(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxEventSerializer outboxEventSerializer(SmartGridKafkaProperties properties) {
        return new OutboxEventSerializer(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxRelayKafkaTemplate")
    public KafkaTemplate<String, byte[]> outboxRelayKafkaTemplate(SmartGridKafkaProperties properties) {
        return new KafkaTemplate<>(OutboxRelayProducerFactory.create(properties));
    }
}
