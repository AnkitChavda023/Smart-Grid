package com.smartgrid.notificationservice.service.channel;

import com.smartgrid.notificationservice.domain.Notification;
import com.smartgrid.notificationservice.domain.NotificationChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// No real mail provider is wired up (no SMTP/SendGrid credentials in scope for local dev) — this records
// delivery intent so the strategy-selection logic itself is fully real and observable.
@Component
public class EmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        log.info("EMAIL to user={} subject=\"{}\" body=\"{}\"", notification.getUserId(), notification.getTitle(), notification.getBody());
    }
}
