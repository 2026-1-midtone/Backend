package com.midtone.backend.user.domain;

import java.time.LocalTime;

public enum NotificationType {
    NAP(LocalTime.of(18, 0)),
    CAFFEINE_CUTOFF(LocalTime.of(0, 0)),
    LIGHT_EXPOSURE(LocalTime.of(21, 0));

    private final LocalTime defaultSuggestedTime;

    NotificationType(LocalTime defaultSuggestedTime) {
        this.defaultSuggestedTime = defaultSuggestedTime;
    }

    public LocalTime getDefaultSuggestedTime() {
        return defaultSuggestedTime;
    }
}
