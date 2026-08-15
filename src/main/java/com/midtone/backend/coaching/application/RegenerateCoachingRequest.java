package com.midtone.backend.coaching.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegenerateCoachingRequest(
        @NotBlank(message = "재생성 시작일은 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "재생성 시작일은 yyyy-MM-dd 형식이어야 합니다.")
        String from,
        @NotBlank(message = "재생성 종료일은 필수 입력값입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "재생성 종료일은 yyyy-MM-dd 형식이어야 합니다.")
        String to
) {}
