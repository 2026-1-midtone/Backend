package com.midtone.backend.auth.application;

import com.midtone.backend.user.application.profile.UserResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser,
        UserResponse user,
        boolean onboardingRequired
) {
}
