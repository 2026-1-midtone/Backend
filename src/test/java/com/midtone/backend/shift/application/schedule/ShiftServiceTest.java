package com.midtone.backend.shift.application.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
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
    @Mock
    private ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger;

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

        ShiftException exception = assertThrows(ShiftException.class, () -> shiftService.createShift(request));
        assertEquals(ShiftException.ErrorCode.DUPLICATE_SHIFT, exception.getErrorCode());
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
                        1L, LocalDate.of(2026, 7, 18), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(shift));

        ShiftListResponse response = shiftService.getShifts(request);

        assertEquals(1, response.shifts().size());
        assertEquals("2026-08-10", response.shifts().get(0).workDate());
    }

    @Test
    void 근무_목록_조회시_전환일을_표시한다() {
        GetShiftsRequest request = new GetShiftsRequest("2026-08-10", "2026-08-31");
        ShiftSchedule priorNight = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 9), ShiftType.NIGHT, new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        ShiftSchedule day = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 10), ShiftType.DAY, new ShiftTime(LocalTime.of(7, 0), LocalTime.of(15, 0)));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(priorNight, day));

        ShiftListResponse response = shiftService.getShifts(request);

        assertEquals(1, response.shifts().size());
        assertEquals("2026-08-10", response.shifts().get(0).workDate());
        assertTrue(response.shifts().get(0).isTransitionDay());
    }

    @Test
    void 조회_결과가_없으면_빈_목록을_반환한다() {
        GetShiftsRequest request = new GetShiftsRequest("2026-09-01", "2026-09-30");
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 8, 18), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of());

        ShiftListResponse response = shiftService.getShifts(request);

        assertTrue(response.shifts().isEmpty());
    }

    @Test
    void to가_from보다_빠르면_예외를_던진다() {
        GetShiftsRequest request = new GetShiftsRequest("2026-08-31", "2026-08-01");

        ShiftException exception = assertThrows(ShiftException.class, () -> shiftService.getShifts(request));
        assertEquals(ShiftException.ErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
    }

    @Test
    void 근무_일정을_수정한다() {
        ShiftSchedule shift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 5), ShiftType.OFF, new ShiftTime(null, null));
        UpdateShiftRequest request = new UpdateShiftRequest("EVENING", "14:00", "22:00");
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByIdAndUserId(505L, 1L)).thenReturn(Optional.of(shift));
        when(shiftCoachingRegenerationTrigger.triggerForSingleUpdate(LocalDate.of(2026, 8, 5)))
                .thenReturn(List.of("2026-08-05", "2026-08-06"));

        UpdateShiftResponse response = shiftService.updateShift(505L, request);

        assertEquals("EVENING", response.shiftType());
        assertEquals("14:00", response.startTime());
        assertEquals("22:00", response.endTime());
        assertEquals(List.of("2026-08-05", "2026-08-06"), response.affectedCoachingDates());
    }

    @Test
    void 존재하지_않는_일정을_수정하면_예외를_던진다() {
        UpdateShiftRequest request = new UpdateShiftRequest("EVENING", "14:00", "22:00");
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        ShiftException exception = assertThrows(ShiftException.class, () -> shiftService.updateShift(999L, request));
        assertEquals(ShiftException.ErrorCode.SHIFT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 근무_일정을_삭제한다() {
        ShiftSchedule shift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 5), ShiftType.OFF, new ShiftTime(null, null));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findById(505L)).thenReturn(Optional.of(shift));

        shiftService.deleteShift(505L);

        verify(shiftScheduleRepository).delete(shift);
    }

    @Test
    void 존재하지_않는_일정을_삭제하면_예외를_던진다() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        ShiftException exception = assertThrows(ShiftException.class, () -> shiftService.deleteShift(999L));
        assertEquals(ShiftException.ErrorCode.SHIFT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 타인의_일정을_삭제하면_예외를_던진다() {
        ShiftSchedule shift = new ShiftSchedule(
                2L, LocalDate.of(2026, 8, 5), ShiftType.OFF, new ShiftTime(null, null));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findById(505L)).thenReturn(Optional.of(shift));

        ShiftException exception = assertThrows(ShiftException.class, () -> shiftService.deleteShift(505L));
        assertEquals(ShiftException.ErrorCode.SHIFT_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void 기간_내_근무_유형을_일괄_변경한다() {
        BulkUpdateShiftRequest request = new BulkUpdateShiftRequest("2026-08-10", "2026-08-14", "NIGHT");
        ShiftSchedule shift1 = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 10), ShiftType.DAY, new ShiftTime(null, null));
        ShiftSchedule shift2 = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 11), ShiftType.DAY, new ShiftTime(null, null));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 14)))
                .thenReturn(List.of(shift1, shift2));
        when(shiftCoachingRegenerationTrigger.triggerForRange(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 14)))
                .thenReturn(List.of("2026-08-10", "2026-08-11"));

        BulkUpdateShiftResponse response = shiftService.bulkUpdateShifts(request);

        assertEquals(2, response.updatedCount());
        assertEquals(ShiftType.NIGHT, shift1.getShiftType());
        assertEquals(ShiftType.NIGHT, shift2.getShiftType());
        assertEquals(List.of("2026-08-10", "2026-08-11"), response.affectedCoachingDates());
    }

    @Test
    void 변경_기간이_90일을_초과하면_예외를_던진다() {
        BulkUpdateShiftRequest request = new BulkUpdateShiftRequest("2026-01-01", "2026-06-01", "NIGHT");

        ShiftException exception =
                assertThrows(ShiftException.class, () -> shiftService.bulkUpdateShifts(request));
        assertEquals(ShiftException.ErrorCode.BULK_UPDATE_RANGE_EXCEEDED, exception.getErrorCode());
    }

    @Test
    void 변경_종료일이_시작일보다_빠르면_예외를_던진다() {
        BulkUpdateShiftRequest request = new BulkUpdateShiftRequest("2026-08-14", "2026-08-10", "NIGHT");

        ShiftException exception =
                assertThrows(ShiftException.class, () -> shiftService.bulkUpdateShifts(request));
        assertEquals(ShiftException.ErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
    }
}
