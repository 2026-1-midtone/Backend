package com.midtone.backend.shift.application.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BulkUpdateShiftRequest(
        @NotBlank(message = "변경 시작일은 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "변경 시작일은 yyyy-MM-dd 형식이어야 합니다.")
        String from,
        @NotBlank(message = "변경 종료일은 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "변경 종료일은 yyyy-MM-dd 형식이어야 합니다.")
        String to,
        @NotBlank(message = "근무 유형은 필수 입력값입니다.")
        @Pattern(regexp = "DAY|EVENING|NIGHT|OFF", message = "근무 유형은 DAY, EVENING, NIGHT, OFF 중 하나여야 합니다.")
        String shiftType
) {}
