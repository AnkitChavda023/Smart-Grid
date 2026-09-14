package com.smartgrid.notificationservice.service.channel;

import com.smartgrid.notificationservice.domain.Notification;
import com.smartgrid.notificationservice.domain.NotificationChannelType;
import com.smartgrid.notificationservice.dto.NotificationPushMessage;
import com.smartgrid.notificationservice.service.RedisFanoutPublisher;
import org.springframework.stereotype.Component;

@Component
public class WebSocketChannel implements NotificationChannel {

    private final RedisFanoutPublisher fanoutPublisher;

    public WebSocketChannel(RedisFanoutPublisher fanoutPublisher) {
        this.fanoutPublisher = fanoutPublisher;
    }

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.WEBSOCKET;
    }

    @Override
    public void send(Notification notification) {
        fanoutPublisher.publish(new NotificationPushMessage(
                notification.getId().toString(), notification.getTitle(), notification.getBody(), notification.getRelatedOrderId()));
    }
}
