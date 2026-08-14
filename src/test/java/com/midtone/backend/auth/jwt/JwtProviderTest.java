package com.midtone.backend.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "FIoq16iU6vxl3B0mtAUAJcW/8IhxbHMkhU9Yo8LGQsw=",
            "shiftrhythm-api",
            Duration.ofMinutes(30),
            Duration.ofDays(30)
    );

    @Test
    void 액세스_토큰을_발급하고_검증한다() {
        String token = jwtProvider.createAccessToken(1L);

        assertTrue(jwtProvider.isValid(token));
        assertTrue(jwtProvider.isAccessToken(token));
        assertEquals(1L, jwtProvider.getUserId(token));
        assertEquals(TokenType.ACCESS, jwtProvider.getTokenType(token));
    }

    @Test
    void 리프레시_토큰을_발급하고_검증한다() {
        String token = jwtProvider.createRefreshToken(2L);

        assertTrue(jwtProvider.isValid(token));
        assertTrue(jwtProvider.isRefreshToken(token));
        assertEquals(2L, jwtProvider.getUserId(token));
        assertEquals(TokenType.REFRESH, jwtProvider.getTokenType(token));
    }

    @Test
    void 잘못된_토큰은_유효하지_않다() {
        assertFalse(jwtProvider.isValid("invalid.token.value"));
    }
}
