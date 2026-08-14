package com.midtone.backend.user;

import com.midtone.backend.user.application.notification.NotificationSettingService;
import com.midtone.backend.user.application.notification.NotificationSettingsResponse;
import com.midtone.backend.user.application.notification.SaveNotificationSettingsRequest;
import com.midtone.backend.user.application.notification.SaveNotificationSettingsResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/notification-settings")
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    public NotificationSettingController(NotificationSettingService notificationSettingService) {
        this.notificationSettingService = notificationSettingService;
    }

    @GetMapping
    public ResponseEntity<NotificationSettingsResponse> getSettings() {
        NotificationSettingsResponse response = notificationSettingService.getSettings();
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<SaveNotificationSettingsResponse> saveSettings(
            @Valid @RequestBody SaveNotificationSettingsRequest request) {
        SaveNotificationSettingsResponse response = notificationSettingService.saveSettings(request);
        return ResponseEntity.ok(response);
    }
}
