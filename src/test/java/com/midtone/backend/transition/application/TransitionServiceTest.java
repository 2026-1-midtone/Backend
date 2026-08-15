package com.midtone.backend.transition.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransitionServiceTest {

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;
    @Mock
    private ShiftScheduleRepository shiftScheduleRepository;
    @Mock
    private TransitionDetector transitionDetector;
    @Mock
    private TransitionProtocolCatalog transitionProtocolCatalog;

    @InjectMocks
    private TransitionService transitionService;

    @Test
    void 근무_유형이_바뀌면_전환일로_기록한다() {
        ShiftSchedule night = new ShiftSchedule(1L, LocalDate.of(2026, 8, 8), ShiftType.NIGHT, new ShiftTime(null, null));
        ShiftSchedule day = new ShiftSchedule(1L, LocalDate.of(2026, 8, 9), ShiftType.DAY, new ShiftTime(null, null));
        TransitionProtocol protocol = new TransitionProtocol("나이트 → 데이 전환", "설명", List.of());
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 7, 26), LocalDate.of(2026, 8, 9)))
                .thenReturn(List.of(night, day));
        when(transitionProtocolCatalog.resolve(ShiftType.NIGHT, ShiftType.DAY)).thenReturn(protocol);

        TransitionListResponse response =
                transitionService.getTransitions(LocalDate.of(2026, 8, 9), LocalDate.of(2026, 8, 9));

        assertEquals(1, response.transitions().size());
        TransitionListResponse.Item item = response.transitions().get(0);
        assertEquals("2026-08-09", item.transitionDate());
        assertEquals("NIGHT", item.fromShiftType());
        assertEquals("DAY", item.toShiftType());
        assertEquals("나이트 → 데이 전환", item.protocolName());
    }

    @Test
    void OFF를_건너뛰고_전환을_감지한다() {
        ShiftSchedule night = new ShiftSchedule(1L, LocalDate.of(2026, 8, 7), ShiftType.NIGHT, new ShiftTime(null, null));
        ShiftSchedule off = new ShiftSchedule(1L, LocalDate.of(2026, 8, 8), ShiftType.OFF, new ShiftTime(null, null));
        ShiftSchedule day = new ShiftSchedule(1L, LocalDate.of(2026, 8, 9), ShiftType.DAY, new ShiftTime(null, null));
        TransitionProtocol protocol = new TransitionProtocol("나이트 → 데이 전환", "설명", List.of());
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 7, 26), LocalDate.of(2026, 8, 9)))
                .thenReturn(List.of(night, off, day));
        when(transitionProtocolCatalog.resolve(ShiftType.NIGHT, ShiftType.DAY)).thenReturn(protocol);

        TransitionListResponse response =
                transitionService.getTransitions(LocalDate.of(2026, 8, 9), LocalDate.of(2026, 8, 9));

        assertEquals(1, response.transitions().size());
        assertEquals("NIGHT", response.transitions().get(0).fromShiftType());
    }

    @Test
    void 조회_범위_이전의_전환은_결과에서_제외한다() {
        ShiftSchedule night = new ShiftSchedule(1L, LocalDate.of(2026, 8, 8), ShiftType.NIGHT, new ShiftTime(null, null));
        ShiftSchedule day9 = new ShiftSchedule(1L, LocalDate.of(2026, 8, 9), ShiftType.DAY, new ShiftTime(null, null));
        ShiftSchedule day10 = new ShiftSchedule(1L, LocalDate.of(2026, 8, 10), ShiftType.DAY, new ShiftTime(null, null));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 10)))
                .thenReturn(List.of(night, day9, day10));

        TransitionListResponse response =
                transitionService.getTransitions(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10));

        assertTrue(response.transitions().isEmpty());
    }

    @Test
    void 전환_가이드를_정상_조회한다() {
        LocalDate date = LocalDate.of(2026, 8, 9);
        ShiftSchedule day = new ShiftSchedule(1L, date, ShiftType.DAY, new ShiftTime(null, null));
        TransitionDetector.TransitionInfo info = new TransitionDetector.TransitionInfo(ShiftType.NIGHT, ShiftType.DAY);
        TransitionProtocol protocol = new TransitionProtocol("나이트 → 데이 전환", "설명", List.of());
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, date)).thenReturn(Optional.of(day));
        when(transitionDetector.detectTransition(1L, date, ShiftType.DAY)).thenReturn(Optional.of(info));
        when(transitionProtocolCatalog.resolve(ShiftType.NIGHT, ShiftType.DAY)).thenReturn(protocol);

        TransitionGuideResponse response = transitionService.getTransitionGuide(date);

        assertEquals("2026-08-09", response.transitionDate());
        assertEquals("NIGHT", response.fromShiftType());
        assertEquals("DAY", response.toShiftType());
        assertEquals("나이트 → 데이 전환", response.protocolName());
    }

    @Test
    void 근무_일정이_없으면_전환일이_아니라는_예외를_던진다() {
        LocalDate date = LocalDate.of(2026, 8, 9);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, date)).thenReturn(Optional.empty());

        TransitionException exception =
                assertThrows(TransitionException.class, () -> transitionService.getTransitionGuide(date));
        assertEquals(TransitionException.ErrorCode.NOT_A_TRANSITION_DAY, exception.getErrorCode());
    }

    @Test
    void 전환일이_아니면_예외를_던진다() {
        LocalDate date = LocalDate.of(2026, 8, 9);
        ShiftSchedule day = new ShiftSchedule(1L, date, ShiftType.DAY, new ShiftTime(null, null));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, date)).thenReturn(Optional.of(day));
        when(transitionDetector.detectTransition(1L, date, ShiftType.DAY)).thenReturn(Optional.empty());

        TransitionException exception =
                assertThrows(TransitionException.class, () -> transitionService.getTransitionGuide(date));
        assertEquals(TransitionException.ErrorCode.NOT_A_TRANSITION_DAY, exception.getErrorCode());
    }
}
