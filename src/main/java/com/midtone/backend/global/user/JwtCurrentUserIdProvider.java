package com.midtone.backend.global.user;

import com.midtone.backend.global.error.UnauthenticatedException;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class JwtCurrentUserIdProvider implements CurrentUserIdProvider {

    @Override
    public long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof Long userId)) {
            throw new UnauthenticatedException();
        }
        return userId;
    }
}
