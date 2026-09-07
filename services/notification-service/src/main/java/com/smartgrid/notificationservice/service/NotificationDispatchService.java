package com.smartgrid.notificationservice.service;

import com.smartgrid.notificationservice.domain.Notification;
import com.smartgrid.notificationservice.domain.NotificationChannelType;
import com.smartgrid.notificationservice.domain.UserNotificationPreference;
import com.smartgrid.notificationservice.repository.NotificationRepository;
import com.smartgrid.notificationservice.repository.UserNotificationPreferenceRepository;
import com.smartgrid.notificationservice.service.channel.NotificationChannelResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDispatchService {

    private static final NotificationChannelType DEFAULT_CHANNEL = NotificationChannelType.WEBSOCKET;

    private final NotificationDedupService dedupService;
    private final NotificationRepository notificationRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final NotificationChannelResolver channelResolver;
    private final NotificationBadgeService badgeService;

    public NotificationDispatchService(
            NotificationDedupService dedupService,
            NotificationRepository notificationRepository,
            UserNotificationPreferenceRepository preferenceRepository,
            NotificationChannelResolver channelResolver,
            NotificationBadgeService badgeService
    ) {
        this.dedupService = dedupService;
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.channelResolver = channelResolver;
        this.badgeService = badgeService;
    }

    @Transactional
    public void dispatch(String userId, String eventId, String title, String body, String relatedOrderId) {
        if (!dedupService.tryClaim(eventId, userId)) {
            return;
        }

        Notification notification = notificationRepository.save(
                new Notification(userId, eventId, title, body, relatedOrderId));
        badgeService.increment(userId);

        NotificationChannelType preferredChannel = preferenceRepository.findById(userId)
                .map(UserNotificationPreference::getChannel)
                .orElse(DEFAULT_CHANNEL);

        channelResolver.resolve(preferredChannel).send(notification);
    }

    // No subscription/assignment model exists anywhere in this system — fanning out to every
    // registered user is the documented simplification used for every Kafka-driven notification
    // path (see NotificationEventListener); ad-hoc agent-triggered notifications follow the same rule.
    @Transactional
    public void dispatchToAll(String eventId, String title, String body, String relatedEntityId) {
        for (UserNotificationPreference preference : preferenceRepository.findAll()) {
            dispatch(preference.getUserId(), eventId, title, body, relatedEntityId);
        }
    }

    @Transactional
    public void markRead(String userId, java.util.UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
        badgeService.clear(userId);
    }
}
