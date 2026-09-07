package com.smartgrid.notificationservice.repository;

import com.smartgrid.notificationservice.domain.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, String> {
}
