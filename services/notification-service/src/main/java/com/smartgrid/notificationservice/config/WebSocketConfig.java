package com.smartgrid.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Plain endpoint for raw-WebSocket clients (e.g. NotificationServiceTest's StandardWebSocketClient).
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:*");
        // Separate SockJS-enabled endpoint for the browser dashboard (M13): SockJS negotiates its own
        // URL scheme (/ws-sockjs/{server}/{session}/websocket), incompatible with a bare raw-WS upgrade
        // at the same path, so it gets its own registration rather than replacing the one above.
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("http://localhost:*")
                .withSockJS();
    }
}
