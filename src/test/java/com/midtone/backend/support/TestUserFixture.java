package com.midtone.backend.support;

import com.midtone.backend.user.domain.User;
import com.midtone.backend.user.domain.UserRepository;
import com.midtone.backend.user.domain.UserSettings;
import com.midtone.backend.user.domain.UserSettingsRepository;

public class TestUserFixture {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;

    public TestUserFixture(UserRepository userRepository, UserSettingsRepository userSettingsRepository) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
    }

    public User createUserWithSettings(String googleSubject) {
        User user = userRepository.save(new User(
                googleSubject, googleSubject + "@test.com", "테스트유저", null));
        userSettingsRepository.save(new UserSettings(
                user.getId(), UserSettings.DEFAULT_PREFERRED_NAP_MINUTES, UserSettings.DEFAULT_MAX_NAPS_PER_DAY));
        return user;
    }
}
