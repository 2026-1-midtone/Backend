package com.midtone.backend.auth.application;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "idToken은 필수 입력값입니다.") String idToken,
        String timezone
) {
}
