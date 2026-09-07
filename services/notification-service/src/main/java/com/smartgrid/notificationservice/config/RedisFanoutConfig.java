package com.smartgrid.notificationservice.config;

import com.smartgrid.notificationservice.service.RedisFanoutPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisFanoutConfig {

    @Bean
    public RedisMessageListenerContainer redisFanoutListenerContainer(
            RedisConnectionFactory connectionFactory, RedisFanoutRelay relay) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(relay, new ChannelTopic(RedisFanoutPublisher.CHANNEL));
        return container;
    }
}
