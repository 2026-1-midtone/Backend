package com.midtone.backend.auth;

public class UnauthenticatedException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "로그인이 필요합니다.";

    public UnauthenticatedException() {
        super(DEFAULT_MESSAGE);
    }
}
