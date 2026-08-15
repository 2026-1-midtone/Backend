package com.midtone.backend.shift.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock
    private ShiftScheduleRepository shiftScheduleRepository;
    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private ShiftService shiftService;

    @Test
    void 근무_일정을_등록한다() {
        CreateShiftRequest request = new CreateShiftRequest("2026-08-29", "NIGHT", "22:00", "07:00");
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.existsByUserIdAndWorkDate(1L, LocalDate.of(2026, 8, 29))).thenReturn(false);

        CreateShiftResponse response = shiftService.createShift(request);

        assertEquals("2026-08-29", response.workDate());
        assertEquals("NIGHT", response.shiftType());
        assertEquals("22:00", response.startTime());
        assertEquals("07:00", response.endTime());
        assertEquals("MANUAL", response.source());
        assertEquals(true, response.confirmed());
    }

    @Test
    void 시작_종료_시각이_없는_OFF_일정도_등록된다() {
        CreateShiftRequest request = new CreateShiftRequest("2026-08-05", "OFF", null, null);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.existsByUserIdAndWorkDate(1L, LocalDate.of(2026, 8, 5))).thenReturn(false);

        CreateShiftResponse response = shiftService.createShift(request);

        assertEquals("OFF", response.shiftType());
        assertNull(response.startTime());
        assertNull(response.endTime());
    }

    @Test
    void 같은_날짜에_이미_일정이_있으면_예외를_던진다() {
        CreateShiftRequest request = new CreateShiftRequest("2026-08-29", "NIGHT", "22:00", "07:00");
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.existsByUserIdAndWorkDate(1L, LocalDate.of(2026, 8, 29))).thenReturn(true);

        assertThrows(DuplicateShiftException.class, () -> shiftService.createShift(request));
    }
}
