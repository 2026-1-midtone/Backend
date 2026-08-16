package com.midtone.backend.auth.application;

import com.midtone.backend.global.validation.Timezone;
import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "idToken은 필수 입력값입니다.") String idToken,
        @Timezone(message = "타임존은 Asia/Seoul과 같은 유효한 값이어야 합니다.") String timezone
) {
}
