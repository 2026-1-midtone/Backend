package com.midtone.backend.user.application;

import com.midtone.backend.user.domain.User;

public record UpdateProfileResponse(Long id, String nickname, String timezone) {

    public static UpdateProfileResponse from(User user) {
        return new UpdateProfileResponse(user.getId(), user.getNickname(), user.getTimezone());
    }
}
