package com.midtone.backend.nap.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        NapException exception = assertThrows(NapException.class, () -> napService.startNap(20));
        assertEquals(NapException.ErrorCode.DAILY_LIMIT_EXCEEDED, exception.getErrorCode());
    }

    @Test
    void 이미_진행중인_낮잠이_있으면_예외를_던진다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(napSessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(1L, NapStatus.RUNNING))
                .willReturn(java.util.Optional.of(org.mockito.Mockito.mock(com.midtone.backend.nap.domain.NapSession.class)));

        NapService napService = new NapService(currentUserIdProvider, napSessionRepository, userSettingsRepository);

        NapException exception = assertThrows(NapException.class, () -> napService.startNap(20));
        assertEquals(NapException.ErrorCode.ALREADY_RUNNING, exception.getErrorCode());
    }

    @Test
    void 존재하지_않는_낮잠_세션을_종료하려하면_예외를_던진다() {
        given(napSessionRepository.findById(99L)).willReturn(java.util.Optional.empty());

        NapService napService = new NapService(currentUserIdProvider, napSessionRepository, userSettingsRepository);

        NapException exception = assertThrows(NapException.class, () -> napService.finishNap(99L, "COMPLETED"));
        assertEquals(NapException.ErrorCode.NAP_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 잘못된_상태값으로_종료하면_예외를_던진다() {
        NapService napService = new NapService(currentUserIdProvider, napSessionRepository, userSettingsRepository);

        NapException exception = assertThrows(NapException.class, () -> napService.finishNap(1L, "RUNNING"));
        assertEquals(NapException.ErrorCode.INVALID_STATUS, exception.getErrorCode());
    }
}
