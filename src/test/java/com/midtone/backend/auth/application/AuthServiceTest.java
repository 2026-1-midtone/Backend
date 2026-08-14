package com.midtone.backend.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.midtone.backend.auth.UnauthenticatedException;
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

    @Test
    void 유효한_리프레시_토큰이면_토큰을_재발급한다() {
        ReissueRequest request = new ReissueRequest("refresh-token");
        stubValidRefreshToken(1L, "refresh-token");
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.of("refresh-token"));
        when(jwtProvider.createAccessToken(1L)).thenReturn("new-access-token");
        when(jwtProvider.createRefreshToken(1L)).thenReturn("new-refresh-token");
        when(jwtProvider.getRefreshTokenExpiration()).thenReturn(Duration.ofDays(30));

        TokenResponse response = authService.reissue(request);

        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
    }

    @Test
    void 유효하지_않은_리프레시_토큰이면_예외를_던진다() {
        ReissueRequest request = new ReissueRequest("invalid-token");
        when(jwtProvider.isValid("invalid-token")).thenReturn(false);

        assertThrows(InvalidRefreshTokenException.class, () -> authService.reissue(request));
    }

    @Test
    void 저장된_리프레시_토큰과_다르면_예외를_던진다() {
        ReissueRequest request = new ReissueRequest("refresh-token");
        stubValidRefreshToken(1L, "refresh-token");
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.of("other-token"));

        assertThrows(InvalidRefreshTokenException.class, () -> authService.reissue(request));
    }

    @Test
    void 유효한_리프레시_토큰으로_로그아웃하면_저장된_토큰을_삭제한다() {
        LogoutRequest request = new LogoutRequest("refresh-token");
        stubValidRefreshToken(1L, "refresh-token");

        authService.logout(request);

        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void 유효하지_않은_리프레시_토큰으로_로그아웃하면_예외를_던진다() {
        LogoutRequest request = new LogoutRequest("invalid-token");
        when(jwtProvider.isValid("invalid-token")).thenReturn(false);

        assertThrows(UnauthenticatedException.class, () -> authService.logout(request));
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

    private void stubValidRefreshToken(long userId, String refreshToken) {
        when(jwtProvider.isValid(refreshToken)).thenReturn(true);
        when(jwtProvider.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtProvider.getUserId(refreshToken)).thenReturn(userId);
    }

    private User newUserWithId(long id) {
        User user = new User("google-1", "user@test.com", "닉네임", null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
