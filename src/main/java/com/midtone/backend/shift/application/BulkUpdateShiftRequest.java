package com.midtone.backend.shift.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BulkUpdateShiftRequest(
        @NotBlank(message = "from은 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "from은 yyyy-MM-dd 형식이어야 합니다.")
        String from,
        @NotBlank(message = "to는 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "to는 yyyy-MM-dd 형식이어야 합니다.")
        String to,
        @NotBlank(message = "shiftType은 필수 입력값입니다.")
        @Pattern(regexp = "DAY|EVENING|NIGHT|OFF", message = "shiftType은 DAY, EVENING, NIGHT, OFF 중 하나여야 합니다.")
        String shiftType
) {}
