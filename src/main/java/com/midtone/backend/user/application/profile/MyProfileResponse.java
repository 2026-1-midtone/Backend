package com.midtone.backend.user.application.profile;

import com.midtone.backend.user.domain.User;
import java.time.LocalDateTime;

public record MyProfileResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        String timezone,
        LocalDateTime createdAt
) {

    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getTimezone(),
                user.getCreatedAt()
        );
    }
}
