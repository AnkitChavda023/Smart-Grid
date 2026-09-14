package com.smartgrid.notificationservice.web;

import com.smartgrid.notificationservice.dto.AdhocNotificationRequest;
import com.smartgrid.notificationservice.dto.MarkReadRequest;
import com.smartgrid.notificationservice.dto.NotificationResponse;
import com.smartgrid.notificationservice.repository.NotificationRepository;
import com.smartgrid.notificationservice.service.NotificationBadgeService;
import com.smartgrid.notificationservice.service.NotificationDispatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService dispatchService;
    private final NotificationBadgeService badgeService;

    public NotificationController(
            NotificationRepository notificationRepository,
            NotificationDispatchService dispatchService,
            NotificationBadgeService badgeService
    ) {
        this.notificationRepository = notificationRepository;
        this.dispatchService = dispatchService;
        this.badgeService = badgeService;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestParam String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @GetMapping("/badge")
    public ResponseEntity<Long> badge(@RequestParam String userId) {
        return ResponseEntity.ok(badgeService.current(userId));
    }

    /** Fan-out notification with no dedicated domain event to trigger off of — see NotificationDispatchService.dispatchToAll. */
    @PostMapping("/adhoc")
    public ResponseEntity<Void> adhoc(@Valid @RequestBody AdhocNotificationRequest request) {
        dispatchService.dispatchToAll(UUID.randomUUID().toString(), request.title(), request.body(), request.relatedEntityId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/mark-read")
    public ResponseEntity<Void> markRead(@Valid @RequestBody MarkReadRequest request) {
        dispatchService.markRead(request.userId(), request.notificationId());
        return ResponseEntity.noContent().build();
    }
}
