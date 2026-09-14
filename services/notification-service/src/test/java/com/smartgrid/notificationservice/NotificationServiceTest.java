package com.smartgrid.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrid.notificationservice.domain.NotificationChannelType;
import com.smartgrid.notificationservice.dto.MarkReadRequest;
import com.smartgrid.notificationservice.dto.NotificationPushMessage;
import com.smartgrid.notificationservice.dto.NotificationResponse;
import com.smartgrid.notificationservice.dto.PreferenceRequest;
import com.smartgrid.notificationservice.service.NotificationDispatchService;
import com.smartgrid.notificationservice.service.RedisFanoutPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationServiceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationDispatchService dispatchService;

    @LocalServerPort
    private int port;

    @Test
    void crossPodFanOutDeliversToConnectedWebSocketClient() throws Exception {
        String orderId = "order-" + UUID.randomUUID();
        BlockingQueue<NotificationPushMessage> messages = subscribeToOrderUpdates(orderId);

        // Simulates "pod B": publishes directly to the Redis pub/sub channel, completely bypassing this
        // instance's own Kafka-consumer path, exactly as an independent pod's consumer would.
        NotificationPushMessage published = new NotificationPushMessage(
                UUID.randomUUID().toString(), "Order rerouted", "Simulated cross-pod message", orderId);
        redisTemplate.convertAndSend(RedisFanoutPublisher.CHANNEL, objectMapper.writeValueAsString(published));

        NotificationPushMessage received = messages.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.body()).isEqualTo("Simulated cross-pod message");
    }

    @Test
    void duplicateNotificationForSameEventIdIsDropped() {
        String userId = "user-" + UUID.randomUUID();
        String eventId = "event-" + UUID.randomUUID();

        dispatchService.dispatch(userId, eventId, "Title", "Body", null);
        dispatchService.dispatch(userId, eventId, "Title", "Body", null);

        NotificationResponse[] notifications = restTemplate.getForObject("/notifications?userId=" + userId, NotificationResponse[].class);
        assertThat(notifications).hasSize(1);
    }

    @Test
    void strategyPatternDeliversToPreferredChannelOnly() throws Exception {
        String orderId = "order-" + UUID.randomUUID();
        String wsUser = "ws-user-" + UUID.randomUUID();
        String emailUser = "email-user-" + UUID.randomUUID();

        setPreference(wsUser, NotificationChannelType.WEBSOCKET);
        setPreference(emailUser, NotificationChannelType.EMAIL);

        BlockingQueue<NotificationPushMessage> messages = subscribeToOrderUpdates(orderId);

        String eventId = "event-" + UUID.randomUUID();
        dispatchService.dispatch(wsUser, eventId + "-ws", "Alert", "For websocket user", orderId);
        dispatchService.dispatch(emailUser, eventId + "-email", "Alert", "For email user", orderId);

        NotificationPushMessage received = messages.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.body()).isEqualTo("For websocket user");

        // The email user's notification was still recorded (dispatch succeeded), just not pushed over WebSocket.
        NotificationResponse[] emailUserNotifications = restTemplate.getForObject("/notifications?userId=" + emailUser, NotificationResponse[].class);
        assertThat(emailUserNotifications).hasSize(1);
    }

    @Test
    void markReadUpdatesDbAndClearsBadgeCount() {
        String userId = "user-" + UUID.randomUUID();
        dispatchService.dispatch(userId, "event-" + UUID.randomUUID(), "Title", "Body", null);

        Long badgeBefore = restTemplate.getForObject("/notifications/badge?userId=" + userId, Long.class);
        assertThat(badgeBefore).isGreaterThan(0);

        NotificationResponse[] notifications = restTemplate.getForObject("/notifications?userId=" + userId, NotificationResponse[].class);
        assertThat(notifications).hasSize(1);
        assertThat(notifications[0].read()).isFalse();

        ResponseEntity<Void> markReadResponse = restTemplate.postForEntity(
                "/notifications/mark-read", new MarkReadRequest(userId, notifications[0].id()), Void.class);
        assertThat(markReadResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Long badgeAfter = restTemplate.getForObject("/notifications/badge?userId=" + userId, Long.class);
        assertThat(badgeAfter).isEqualTo(0);

        NotificationResponse[] afterMarkRead = restTemplate.getForObject("/notifications?userId=" + userId, NotificationResponse[].class);
        assertThat(afterMarkRead[0].read()).isTrue();
    }

    private void setPreference(String userId, NotificationChannelType channel) {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/notification-preferences", new PreferenceRequest(userId, channel), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private BlockingQueue<NotificationPushMessage> subscribeToOrderUpdates(String orderId) throws Exception {
        BlockingQueue<NotificationPushMessage> queue = new LinkedBlockingQueue<>();
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.setTaskScheduler(new ConcurrentTaskScheduler());

        StompSession session = stompClient.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
        }).get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/orders/" + orderId + "/updates", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationPushMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((NotificationPushMessage) payload);
            }
        });

        Thread.sleep(500);
        return queue;
    }
}
