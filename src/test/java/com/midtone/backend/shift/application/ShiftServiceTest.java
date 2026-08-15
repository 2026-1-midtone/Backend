package com.midtone.backend.shift.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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

        ShiftResponse response = shiftService.createShift(request);

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

        ShiftResponse response = shiftService.createShift(request);

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

    @Test
    void 기간_내_근무_일정을_조회한다() {
        GetShiftsRequest request = new GetShiftsRequest("2026-08-01", "2026-08-31");
        ShiftSchedule shift = new ShiftSchedule(
                1L,
                LocalDate.of(2026, 8, 10),
                ShiftType.NIGHT,
                new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(shift));

        ShiftListResponse response = shiftService.getShifts(request);

        assertEquals(1, response.shifts().size());
        assertEquals("2026-08-10", response.shifts().get(0).workDate());
    }

    @Test
    void 조회_결과가_없으면_빈_목록을_반환한다() {
        GetShiftsRequest request = new GetShiftsRequest("2026-09-01", "2026-09-30");
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of());

        ShiftListResponse response = shiftService.getShifts(request);

        assertTrue(response.shifts().isEmpty());
    }

    @Test
    void to가_from보다_빠르면_예외를_던진다() {
        GetShiftsRequest request = new GetShiftsRequest("2026-08-31", "2026-08-01");

        assertThrows(InvalidDateRangeException.class, () -> shiftService.getShifts(request));
    }
}
