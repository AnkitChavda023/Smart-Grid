package com.smartgrid.notificationservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_notification_prefs")
public class UserNotificationPreference {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannelType channel;

    protected UserNotificationPreference() {
    }

    public UserNotificationPreference(String userId, NotificationChannelType channel) {
        this.userId = userId;
        this.channel = channel;
    }

    public String getUserId() {
        return userId;
    }

    public NotificationChannelType getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannelType channel) {
        this.channel = channel;
    }
}
