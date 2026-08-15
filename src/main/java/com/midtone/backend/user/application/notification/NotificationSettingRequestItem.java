package com.midtone.backend.user.application.notification;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NotificationSettingRequestItem(
        @Pattern(regexp = "NAP|CAFFEINE_CUTOFF|LIGHT_EXPOSURE", message = "지원하지 않는 알림 유형입니다.")
        String type,
        @NotNull(message = "enabled는 필수 입력값입니다.") Boolean enabled,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "customTime은 HH:mm 형식이어야 합니다.")
        String customTime
) {
}

