package com.midtone.backend.global.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    Clock applicationClock() {
        return Clock.system(DateTimeDefaults.DEFAULT_ZONE);
    }
}
