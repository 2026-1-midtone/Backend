package com.midtone.backend.user.application.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SaveNotificationSettingsRequest(
        @NotEmpty(message = "settings는 필수 입력값입니다.") @Valid List<NotificationSettingRequestItem> settings
) {
}
