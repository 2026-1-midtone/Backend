package com.midtone.backend.shift.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.domain.ShiftPattern;
import com.midtone.backend.shift.domain.ShiftPatternRepository;
import com.midtone.backend.shift.domain.ShiftType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftPatternServiceTest {

    @Mock
    private ShiftPatternRepository shiftPatternRepository;
    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private ShiftPatternService shiftPatternService;

    @Test
    void 반복_패턴_목록을_조회한다() {
        ShiftPattern shiftPattern = new ShiftPattern(
                1L, "3교대 기본", List.of(ShiftType.DAY, ShiftType.EVENING, ShiftType.NIGHT, ShiftType.OFF));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftPatternRepository.findByUserId(1L)).thenReturn(List.of(shiftPattern));

        ShiftPatternListResponse response = shiftPatternService.getShiftPatterns();

        assertEquals(1, response.patterns().size());
        assertEquals("3교대 기본", response.patterns().get(0).name());
    }

    @Test
    void 반복_패턴을_저장한다() {
        SaveShiftPatternRequest request =
                new SaveShiftPatternRequest("나이트 집중", List.of("NIGHT", "NIGHT", "NIGHT", "OFF", "OFF"));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);

        ShiftPatternResponse response = shiftPatternService.saveShiftPattern(request);

        assertEquals("나이트 집중", response.name());
        assertEquals(5, response.cycleDays());
        assertEquals(List.of("NIGHT", "NIGHT", "NIGHT", "OFF", "OFF"), response.pattern());
    }

    @Test
    void 반복_패턴을_삭제한다() {
        ShiftPattern shiftPattern = new ShiftPattern(1L, "나이트 집중", List.of(ShiftType.NIGHT));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftPatternRepository.findByIdAndUserId(4L, 1L)).thenReturn(Optional.of(shiftPattern));

        shiftPatternService.deleteShiftPattern(4L);

        verify(shiftPatternRepository).delete(shiftPattern);
    }

    @Test
    void 존재하지_않는_패턴을_삭제하면_예외를_던진다() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftPatternRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ShiftPatternNotFoundException.class, () -> shiftPatternService.deleteShiftPattern(999L));
    }
}
