package com.smartgrid.notificationservice.service.channel;

import com.smartgrid.notificationservice.domain.NotificationChannelType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NotificationChannelResolver {

    private final Map<NotificationChannelType, NotificationChannel> channelsByType;

    public NotificationChannelResolver(List<NotificationChannel> channels) {
        this.channelsByType = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::type, Function.identity()));
    }

    public NotificationChannel resolve(NotificationChannelType type) {
        NotificationChannel channel = channelsByType.get(type);
        if (channel == null) {
            throw new IllegalStateException("No NotificationChannel registered for type " + type);
        }
        return channel;
    }
}
