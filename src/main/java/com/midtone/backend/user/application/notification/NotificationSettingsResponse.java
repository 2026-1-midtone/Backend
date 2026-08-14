package com.midtone.backend.user.application.notification;

import java.util.List;

public record NotificationSettingsResponse(List<NotificationSettingResponse> settings) {
}
