package com.midtone.backend.sleep.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;

public record CreateSleepLogRequest(
        @NotNull(message = "sleptAt은 필수 입력값입니다.") OffsetDateTime sleptAt,
        @NotNull(message = "wokeAt은 필수 입력값입니다.") OffsetDateTime wokeAt,
        @Pattern(regexp = "MANUAL|DEVICE", message = "source는 MANUAL 또는 DEVICE여야 합니다.") String source) {
}
