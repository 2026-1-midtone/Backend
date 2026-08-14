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

    @Transactional
    public TokenResponse reissue(ReissueRequest request) {
        String refreshToken = request.refreshToken();
        validateRefreshToken(refreshToken);
        long userId = jwtProvider.getUserId(refreshToken);
        validateTokenMatchesStored(userId, refreshToken);

        return issueTokens(userId);
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

    private void validateRefreshToken(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }
    }

    private void validateTokenMatchesStored(long userId, String refreshToken) {
        String storedRefreshToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!storedRefreshToken.equals(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }
    }

    private LoginResponse issueLoginResponse(User user, boolean isNewUser) {
        TokenResponse tokens = issueTokens(user.getId());
        return new LoginResponse(
                tokens.accessToken(), tokens.refreshToken(), isNewUser, UserResponse.from(user), isNewUser);
    }

    private TokenResponse issueTokens(long userId) {
        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenRepository.save(userId, refreshToken, jwtProvider.getRefreshTokenExpiration());

        return new TokenResponse(accessToken, refreshToken);
    }
}
