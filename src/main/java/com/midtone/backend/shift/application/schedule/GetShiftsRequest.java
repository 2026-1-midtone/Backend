package com.midtone.backend.shift.application.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GetShiftsRequest(
        @NotBlank(message = "조회 시작일은 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "조회 시작일은 yyyy-MM-dd 형식이어야 합니다.")
        String from,
        @NotBlank(message = "조회 종료일은 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "조회 종료일은 yyyy-MM-dd 형식이어야 합니다.")
        String to
) {}
