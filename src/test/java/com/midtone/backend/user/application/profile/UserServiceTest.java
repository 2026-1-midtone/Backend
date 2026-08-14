package com.midtone.backend.user.application.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.midtone.backend.auth.domain.RefreshTokenRepository;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.user.domain.User;
import com.midtone.backend.user.domain.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserIdProvider currentUserIdProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 내_프로필을_조회한다() {
        User user = newUserWithId(1L);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MyProfileResponse response = userService.getMyProfile();

        assertEquals(user.getEmail(), response.email());
        assertEquals(user.getNickname(), response.nickname());
    }

    @Test
    void 존재하지_않는_사용자면_예외를_던진다() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getMyProfile());
    }

    @Test
    void 닉네임과_타임존을_수정한다() {
        User user = newUserWithId(1L);
        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임", "America/New_York");
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UpdateProfileResponse response = userService.updateMyProfile(request);

        assertEquals("새닉네임", response.nickname());
        assertEquals("America/New_York", response.timezone());
    }

    @Test
    void 닉네임만_전달되면_타임존은_유지된다() {
        User user = newUserWithId(1L);
        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임", null);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UpdateProfileResponse response = userService.updateMyProfile(request);

        assertEquals("새닉네임", response.nickname());
        assertEquals("Asia/Seoul", response.timezone());
    }

    @Test
    void 회원_탈퇴하면_유저와_리프레시_토큰을_삭제한다() {
        User user = newUserWithId(1L);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.withdraw();

        verify(refreshTokenRepository).deleteByUserId(1L);
        verify(userRepository).delete(user);
    }

    private User newUserWithId(long id) {
        User user = new User("google-1", "user@test.com", "닉네임", null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
