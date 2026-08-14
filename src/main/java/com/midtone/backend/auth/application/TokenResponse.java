package com.midtone.backend.auth.application;

public record TokenResponse(String accessToken, String refreshToken) {
}
