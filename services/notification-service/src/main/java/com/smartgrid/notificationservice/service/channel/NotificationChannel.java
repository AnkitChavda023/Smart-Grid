package com.smartgrid.notificationservice.service.channel;

import com.smartgrid.notificationservice.domain.Notification;
import com.smartgrid.notificationservice.domain.NotificationChannelType;

public interface NotificationChannel {

    NotificationChannelType type();

    void send(Notification notification);
}
