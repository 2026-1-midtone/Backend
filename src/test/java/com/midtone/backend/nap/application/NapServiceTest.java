package com.midtone.backend.nap.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.nap.domain.NapSessionRepository;
import com.midtone.backend.nap.domain.NapStatus;
import com.midtone.backend.user.domain.UserSettingsRepository;
import com.midtone.backend.user.domain.UserSettings;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NapServiceTest {

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @Mock
    private NapSessionRepository napSessionRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private UserSettings userSettings;

    @Test
    void rejectsNewNapWhenDailyLimitIsReached() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(napSessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(1L, NapStatus.RUNNING)).willReturn(java.util.Optional.empty());
        given(napSessionRepository.countByUserIdAndStartedAtBetween(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .willReturn(2L);
        given(userSettingsRepository.findById(1L)).willReturn(java.util.Optional.of(userSettings));
        given(userSettings.getMaxNapsPerDay()).willReturn(2);

        NapService napService = new NapService(currentUserIdProvider, napSessionRepository, userSettingsRepository);

        assertThatThrownBy(() -> napService.startNap(20))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("오늘 설정한 최대 낮잠 횟수를 모두 사용했어요.");
    }
}
