package com.midtone.backend.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * 개발용 인증 우회(local 프로파일)가 프로파일을 지정하지 않은 배포에 딸려 올라가지 않는지 지킨다.
 */
class DefaultProfileTest {

    private static final String LOCAL_PROFILE = "local";

    @Test
    void defaultProfileIsNotTheAuthBypassProfile() throws IOException {
        assertThat(defaultProfile()).isNotEqualTo(LOCAL_PROFILE);
    }

    private Object defaultProfile() throws IOException {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        return sources.stream()
                .map(source -> source.getProperty("spring.profiles.default"))
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }
}
