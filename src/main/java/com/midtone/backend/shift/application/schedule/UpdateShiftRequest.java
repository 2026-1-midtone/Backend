package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.global.validation.ValidationPatterns;
import jakarta.validation.constraints.Pattern;

public record UpdateShiftRequest(
        @Pattern(regexp = ValidationPatterns.SHIFT_TYPE, message = "근무 유형은 DAY, EVENING, NIGHT, OFF 중 하나여야 합니다.")
        String shiftType,
        @Pattern(regexp = ValidationPatterns.TIME, message = "근무 시작 시각은 HH:mm 형식이어야 합니다.")
        String startTime,
        @Pattern(regexp = ValidationPatterns.TIME, message = "근무 종료 시각은 HH:mm 형식이어야 합니다.")
        String endTime
) {}
