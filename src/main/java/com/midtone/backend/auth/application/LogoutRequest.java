package com.midtone.backend.auth.application;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "refreshToken은 필수 입력값입니다.") String refreshToken
) {
}
