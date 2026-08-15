package com.midtone.backend.user.application.profile;

import com.midtone.backend.auth.domain.RefreshTokenRepository;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.user.application.UserException;
import com.midtone.backend.user.domain.User;
import com.midtone.backend.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserIdProvider currentUserIdProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(
            UserRepository userRepository,
            CurrentUserIdProvider currentUserIdProvider,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.currentUserIdProvider = currentUserIdProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile() {
        return MyProfileResponse.from(getCurrentUser());
    }

    @Transactional
    public UpdateProfileResponse updateMyProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        applyChanges(user, request);
        return UpdateProfileResponse.from(user);
    }

    @Transactional
    public void withdraw() {
        User user = getCurrentUser();
        refreshTokenRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    private void applyChanges(User user, UpdateProfileRequest request) {
        if (request.nickname() != null) {
            user.changeNickname(request.nickname());
        }
        if (request.timezone() != null) {
            user.changeTimezone(request.timezone());
        }
    }

    private User getCurrentUser() {
        long userId = currentUserIdProvider.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserException.ErrorCode.NOT_FOUND));
    }
}
