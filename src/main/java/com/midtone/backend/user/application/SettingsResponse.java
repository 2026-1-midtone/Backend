package com.midtone.backend.user.application;

import com.midtone.backend.user.domain.UserSettings;

public record SettingsResponse(
        Integer caffeineDailyMg,
        String caffeineSensitivity,
        int preferredNapMinutes,
        int maxNapsPerDay
) {

    public static SettingsResponse from(UserSettings settings) {
        String sensitivity = settings.getCaffeineSensitivity() == null
                ? null
                : settings.getCaffeineSensitivity().name();
        return new SettingsResponse(
                settings.getCaffeineDailyMg(), sensitivity, settings.getPreferredNapMinutes(), settings.getMaxNapsPerDay());
    }

    public static SettingsResponse defaults() {
        return new SettingsResponse(
                null, null, UserSettings.DEFAULT_PREFERRED_NAP_MINUTES, UserSettings.DEFAULT_MAX_NAPS_PER_DAY);
    }
}
