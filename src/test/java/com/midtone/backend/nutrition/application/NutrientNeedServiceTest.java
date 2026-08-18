package com.midtone.backend.nutrition.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.nutrition.domain.UserNutrientNeedRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NutrientNeedServiceTest {

    @Mock UserNutrientNeedRepository repository;
    @Mock CurrentUserIdProvider currentUserIdProvider;
    private NutrientNeedService service;

    @BeforeEach
    void setUp() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        service = new NutrientNeedService(repository, currentUserIdProvider,
                Clock.fixed(Instant.parse("2026-08-18T00:00:00Z"), ZoneId.of("Asia/Seoul")));
    }

    @Test
    void 영양소_목표를_전체_교체한다() {
        SaveNutrientNeedsRequest request = new SaveNutrientNeedsRequest(List.of(
                new SaveNutrientNeedsRequest.Item("VITAMIN_D", "HEALTH_CHECK", "2026-08-17"),
                new SaveNutrientNeedsRequest.Item("MAGNESIUM", null, null)));

        service.replace(request);

        verify(repository).deleteAllByUserId(1L);
        verify(repository).flush();
        ArgumentCaptor<List<com.midtone.backend.nutrition.domain.UserNutrientNeed>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals("HEALTH_CHECK", captor.getValue().get(0).getSource().name());
        assertEquals("USER_REPORTED", captor.getValue().get(1).getSource().name());
    }

    @Test
    void 지원하지_않는_영양소는_거절한다() {
        SaveNutrientNeedsRequest request = new SaveNutrientNeedsRequest(List.of(
                new SaveNutrientNeedsRequest.Item("IRON", "USER_REPORTED", null)));

        NutrientException exception = assertThrows(NutrientException.class, () -> service.replace(request));

        assertEquals(NutrientException.ErrorCode.INVALID_NUTRIENT, exception.getErrorCode());
    }
}
