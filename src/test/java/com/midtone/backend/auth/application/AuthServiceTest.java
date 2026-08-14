package com.midtone.backend.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.midtone.backend.auth.domain.RefreshTokenRepository;
import com.midtone.backend.auth.google.GoogleTokenVerifier;
import com.midtone.backend.auth.google.GoogleUserInfo;
import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.user.domain.User;
import com.midtone.backend.user.domain.UserRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void 처음_로그인하면_새_사용자를_생성한다() {
        GoogleLoginRequest request = new GoogleLoginRequest("id-token", null);
        User savedUser = newUserWithId(1L);
        stubGoogleVerification(savedUser);
        when(userRepository.findByGoogleSubject("google-1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        stubTokenIssuance();

        LoginResponse response = authService.loginWithGoogle(request);

        assertTrue(response.isNewUser());
        assertTrue(response.onboardingRequired());
        assertEquals("access-token", response.accessToken());
    }

    @Test
    void 이미_가입한_사용자면_기존_정보로_로그인한다() {
        GoogleLoginRequest request = new GoogleLoginRequest("id-token", null);
        User existingUser = newUserWithId(2L);
        stubGoogleVerification(existingUser);
        when(userRepository.findByGoogleSubject("google-1")).thenReturn(Optional.of(existingUser));
        stubTokenIssuance();

        LoginResponse response = authService.loginWithGoogle(request);

        assertFalse(response.isNewUser());
        assertFalse(response.onboardingRequired());
    }

    private void stubGoogleVerification(User user) {
        GoogleUserInfo googleUserInfo = new GoogleUserInfo(
                "google-1", user.getEmail(), user.getNickname(), user.getProfileImageUrl());
        when(googleTokenVerifier.verify("id-token")).thenReturn(googleUserInfo);
    }

    private void stubTokenIssuance() {
        when(jwtProvider.createAccessToken(anyLong())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(anyLong())).thenReturn("refresh-token");
        when(jwtProvider.getRefreshTokenExpiration()).thenReturn(Duration.ofDays(30));
    }

    private User newUserWithId(long id) {
        User user = new User("google-1", "user@test.com", "닉네임", null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
