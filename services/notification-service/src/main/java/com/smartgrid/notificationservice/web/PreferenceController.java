package com.smartgrid.notificationservice.web;

import com.smartgrid.notificationservice.domain.UserNotificationPreference;
import com.smartgrid.notificationservice.dto.PreferenceRequest;
import com.smartgrid.notificationservice.repository.UserNotificationPreferenceRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification-preferences")
public class PreferenceController {

    private final UserNotificationPreferenceRepository preferenceRepository;

    public PreferenceController(UserNotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @PostMapping
    public ResponseEntity<Void> setPreference(@Valid @RequestBody PreferenceRequest request) {
        preferenceRepository.save(new UserNotificationPreference(request.userId(), request.channel()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
