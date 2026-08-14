package com.midtone.backend.global.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.midtone.backend.global.error.UnauthenticatedException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtCurrentUserIdProviderTest {

    private final JwtCurrentUserIdProvider jwtCurrentUserIdProvider = new JwtCurrentUserIdProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증된_사용자의_아이디를_반환한다() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));

        assertEquals(1L, jwtCurrentUserIdProvider.getCurrentUserId());
    }

    @Test
    void 인증정보가_없으면_예외를_던진다() {
        assertThrows(UnauthenticatedException.class, jwtCurrentUserIdProvider::getCurrentUserId);
    }
}
