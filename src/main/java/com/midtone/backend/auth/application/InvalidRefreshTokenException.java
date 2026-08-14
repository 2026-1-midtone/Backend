package com.midtone.backend.auth.application;

public class InvalidRefreshTokenException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "만료된 리프레시 토큰입니다. 다시 로그인해 주세요.";

    public InvalidRefreshTokenException() {
        super(DEFAULT_MESSAGE);
    }
}
