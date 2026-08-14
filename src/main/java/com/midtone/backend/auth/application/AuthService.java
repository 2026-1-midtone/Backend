package com.midtone.backend.auth.application;

import com.midtone.backend.auth.domain.RefreshTokenRepository;
import com.midtone.backend.auth.google.GoogleTokenVerifier;
import com.midtone.backend.auth.google.GoogleUserInfo;
import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.user.application.UserResponse;
import com.midtone.backend.user.domain.User;
import com.midtone.backend.user.domain.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(
            GoogleTokenVerifier googleTokenVerifier,
            UserRepository userRepository,
            JwtProvider jwtProvider,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(request.idToken());
        Optional<User> existingUser = userRepository.findByGoogleSubject(googleUserInfo.subject());
        boolean isNewUser = existingUser.isEmpty();
        User user = existingUser.orElseGet(() -> createUser(googleUserInfo, request.timezone()));

        return issueLoginResponse(user, isNewUser);
    }

    private User createUser(GoogleUserInfo googleUserInfo, String timezone) {
        User user = new User(
                googleUserInfo.subject(),
                googleUserInfo.email(),
                googleUserInfo.nickname(),
                googleUserInfo.profileImageUrl()
        );
        if (timezone != null) {
            user.changeTimezone(timezone);
        }
        return userRepository.save(user);
    }

    private LoginResponse issueLoginResponse(User user, boolean isNewUser) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken, jwtProvider.getRefreshTokenExpiration());

        return new LoginResponse(accessToken, refreshToken, isNewUser, UserResponse.from(user), isNewUser);
    }
}
