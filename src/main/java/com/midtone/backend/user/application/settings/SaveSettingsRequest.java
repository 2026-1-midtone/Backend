package com.midtone.backend.user.application.settings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SaveSettingsRequest(
        Integer caffeineDailyMg,
        @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "caffeineSensitivity는 LOW, MEDIUM, HIGH 중 하나여야 합니다.")
        String caffeineSensitivity,
        @NotNull(message = "preferredNapMinutes은 필수 입력값입니다.") Integer preferredNapMinutes,
        @NotNull(message = "maxNapsPerDay는 필수 입력값입니다.") Integer maxNapsPerDay
) {
}
