package com.midtone.backend.user.application.profile;

import com.midtone.backend.global.validation.Timezone;
import com.midtone.backend.user.domain.User;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = User.NICKNAME_MAX_LENGTH, message = "닉네임은 50자를 초과할 수 없습니다.") String nickname,
        @Timezone(message = "타임존은 Asia/Seoul과 같은 유효한 값이어야 합니다.") String timezone
) {
}
