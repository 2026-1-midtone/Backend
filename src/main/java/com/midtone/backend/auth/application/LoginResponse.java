package com.midtone.backend.auth.application;

import com.midtone.backend.user.application.UserResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser,
        UserResponse user,
        boolean onboardingRequired
) {
}
