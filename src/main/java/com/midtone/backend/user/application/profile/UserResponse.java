package com.midtone.backend.user.application.profile;

import com.midtone.backend.user.domain.User;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        String timezone
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getTimezone()
        );
    }
}
