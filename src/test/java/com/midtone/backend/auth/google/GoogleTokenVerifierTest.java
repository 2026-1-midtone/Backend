package com.midtone.backend.auth.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.midtone.backend.auth.AuthException;
import org.junit.jupiter.api.Test;

class GoogleTokenVerifierTest {

    private final GoogleTokenVerifier googleTokenVerifier =
            new GoogleTokenVerifier("test-client-id.apps.googleusercontent.com");

    @Test
    void 형식이_올바르지_않은_토큰은_예외를_던진다() {
        AuthException exception = assertThrows(AuthException.class,
                () -> googleTokenVerifier.verify("invalid-token"));
        assertEquals(AuthException.ErrorCode.GOOGLE_TOKEN_VERIFICATION_FAILED, exception.getErrorCode());
    }
}
