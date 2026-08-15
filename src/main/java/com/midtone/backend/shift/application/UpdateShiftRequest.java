package com.midtone.backend.shift.application;

import jakarta.validation.constraints.Pattern;

public record UpdateShiftRequest(
        @Pattern(regexp = "DAY|EVENING|NIGHT|OFF", message = "근무 유형은 DAY, EVENING, NIGHT, OFF 중 하나여야 합니다.")
        String shiftType,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "근무 시작 시각은 HH:mm 형식이어야 합니다.")
        String startTime,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "근무 종료 시각은 HH:mm 형식이어야 합니다.")
        String endTime
) {}
