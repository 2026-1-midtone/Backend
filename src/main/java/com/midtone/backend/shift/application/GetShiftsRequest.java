package com.midtone.backend.shift.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GetShiftsRequest(
        @NotBlank(message = "from은 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "from은 yyyy-MM-dd 형식이어야 합니다.")
        String from,
        @NotBlank(message = "to는 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "to는 yyyy-MM-dd 형식이어야 합니다.")
        String to
) {}
