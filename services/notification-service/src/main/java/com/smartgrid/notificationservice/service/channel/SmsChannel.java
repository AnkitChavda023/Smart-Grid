package com.smartgrid.notificationservice.service.channel;

import com.smartgrid.notificationservice.domain.Notification;
import com.smartgrid.notificationservice.domain.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.SMS;
    }

    @Override
    public void send(Notification notification) {
        log.info("SMS to user={} body=\"{}\"", notification.getUserId(), notification.getBody());
    }
}
