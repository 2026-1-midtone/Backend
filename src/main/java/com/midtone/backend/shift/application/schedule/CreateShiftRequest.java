package com.midtone.backend.shift.application.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateShiftRequest(
        @NotBlank(message = "근무 날짜는 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "근무 날짜는 yyyy-MM-dd 형식이어야 합니다.")
        String workDate,

        @NotBlank(message = "근무 유형은 필수 입력값입니다.")
        @Pattern(regexp = "DAY|EVENING|NIGHT|OFF", message = "근무 유형은 DAY, EVENING, NIGHT, OFF 중 하나여야 합니다.")
        String shiftType,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "근무 시작 시각은 HH:mm 형식이어야 합니다.")
        String startTime,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "근무 종료 시각은 HH:mm 형식이어야 합니다.")
        String endTime
) {
}
