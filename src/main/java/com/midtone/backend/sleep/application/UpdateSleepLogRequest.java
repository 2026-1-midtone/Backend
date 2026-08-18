package com.midtone.backend.sleep.application;

import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;

public record UpdateSleepLogRequest(
        OffsetDateTime sleptAt,
        OffsetDateTime wokeAt,
        @Pattern(regexp = "MANUAL|DEVICE", message = "source는 MANUAL 또는 DEVICE여야 합니다.") String source) {
}
