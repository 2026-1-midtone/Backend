package com.midtone.backend.user.application.notification;

import com.midtone.backend.user.domain.NotificationSetting;
import com.midtone.backend.user.domain.NotificationType;
import java.time.LocalTime;

public record NotificationSettingResponse(String type, boolean enabled, String customTime, String suggestedTime) {

    public static NotificationSettingResponse from(NotificationSetting setting, LocalTime defaultSuggestedTime) {
        return new NotificationSettingResponse(
                setting.getType().name(),
                setting.isEnabled(),
                setting.getCustomTime() == null ? null : setting.getCustomTime().toString(),
                defaultSuggestedTime.toString()
        );
    }

    public static NotificationSettingResponse defaultFor(NotificationType type) {
        return new NotificationSettingResponse(type.name(), true, null, type.getDefaultSuggestedTime().toString());
    }
}
