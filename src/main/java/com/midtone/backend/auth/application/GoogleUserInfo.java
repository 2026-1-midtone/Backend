package com.midtone.backend.auth.application;

public record GoogleUserInfo(
        String subject,
        String email,
        String nickname,
        String profileImageUrl
) {
}
