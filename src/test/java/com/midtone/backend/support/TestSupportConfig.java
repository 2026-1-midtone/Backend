package com.midtone.backend.support;

import com.midtone.backend.user.domain.UserRepository;
import com.midtone.backend.user.domain.UserSettingsRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class TestSupportConfig {

    @Bean
    TestUserFixture testUserFixture(
            UserRepository userRepository, UserSettingsRepository userSettingsRepository) {
        return new TestUserFixture(userRepository, userSettingsRepository);
    }
}
