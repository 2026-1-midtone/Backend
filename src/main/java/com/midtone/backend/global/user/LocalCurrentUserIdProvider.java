package com.midtone.backend.global.user;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalCurrentUserIdProvider implements CurrentUserIdProvider {

    public static final long LOCAL_USER_ID = 1L;

    @Override
    public long getCurrentUserId() {
        return LOCAL_USER_ID;
    }
}
