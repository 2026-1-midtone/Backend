package com.midtone.backend.auth.google;

public record GoogleUserInfo(
        String subject,
        String email,
        String nickname,
        String profileImageUrl
) {
}
