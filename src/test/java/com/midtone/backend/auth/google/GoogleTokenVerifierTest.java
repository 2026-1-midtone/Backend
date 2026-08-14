package com.midtone.backend.auth.google;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GoogleTokenVerifierTest {

    private final GoogleTokenVerifier googleTokenVerifier =
            new GoogleTokenVerifier("test-client-id.apps.googleusercontent.com");

    @Test
    void 형식이_올바르지_않은_토큰은_예외를_던진다() {
        assertThrows(InvalidGoogleTokenException.class,
                () -> googleTokenVerifier.verify("invalid-token"));
    }
}
