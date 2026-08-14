package com.midtone.backend.user.application.settings;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.user.domain.CaffeineSensitivity;
import com.midtone.backend.user.domain.UserSettings;
import com.midtone.backend.user.domain.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public UserSettingsService(
            UserSettingsRepository userSettingsRepository, CurrentUserIdProvider currentUserIdProvider) {
        this.userSettingsRepository = userSettingsRepository;
        this.currentUserIdProvider = currentUserIdProvider;
    }

    @Transactional(readOnly = true)
    public SettingsResponse getSettings() {
        long userId = currentUserIdProvider.getCurrentUserId();
        return userSettingsRepository.findById(userId)
                .map(SettingsResponse::from)
                .orElseGet(SettingsResponse::defaults);
    }

    @Transactional
    public SettingsResponse saveSettings(SaveSettingsRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        UserSettings settings = findOrCreate(userId, request);
        applyChanges(settings, request);
        userSettingsRepository.save(settings);
        return SettingsResponse.from(settings);
    }

    private UserSettings findOrCreate(long userId, SaveSettingsRequest request) {
        return userSettingsRepository.findById(userId)
                .orElseGet(() -> new UserSettings(userId, request.preferredNapMinutes(), request.maxNapsPerDay()));
    }

    private void applyChanges(UserSettings settings, SaveSettingsRequest request) {
        settings.changePreferredNapMinutes(request.preferredNapMinutes());
        settings.changeMaxNapsPerDay(request.maxNapsPerDay());
        settings.changeCaffeineDailyMg(request.caffeineDailyMg());
        settings.changeCaffeineSensitivity(toCaffeineSensitivity(request.caffeineSensitivity()));
    }

    private CaffeineSensitivity toCaffeineSensitivity(String value) {
        return value == null ? null : CaffeineSensitivity.valueOf(value);
    }
}
