package com.midtone.backend.shift.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateShiftRequest(
        @NotBlank(message = "workDate는 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "workDate는 yyyy-MM-dd 형식이어야 합니다.")
        String workDate,

        @NotBlank(message = "shiftType은 필수 입력값입니다.")
        @Pattern(regexp = "DAY|EVENING|NIGHT|OFF", message = "shiftType은 DAY, EVENING, NIGHT, OFF 중 하나여야 합니다.")
        String shiftType,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "startTime은 HH:mm 형식이어야 합니다.")
        String startTime,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "endTime은 HH:mm 형식이어야 합니다.")
        String endTime
) {
}
