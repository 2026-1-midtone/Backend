package com.midtone.backend.user.application.notification;

import com.midtone.backend.user.domain.NotificationSetting;

public record NotificationSettingSaveResponse(String type, boolean enabled, String customTime) {

    public static NotificationSettingSaveResponse from(NotificationSetting setting) {
        return new NotificationSettingSaveResponse(
                setting.getType().name(),
                setting.isEnabled(),
                setting.getCustomTime() == null ? null : setting.getCustomTime().toString()
        );
    }
}
