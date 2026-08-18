package com.midtone.backend.chat.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.user.domain.UserSettingsRepository;
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
class ChatContextBuilderTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 18);

    @Mock
    private ShiftScheduleRepository shiftScheduleRepository;
    @Mock
    private UserSettingsRepository userSettingsRepository;
    @Mock
    private TransitionDetector transitionDetector;

    @InjectMocks
    private ChatContextBuilder chatContextBuilder;

    @Test
    void 오늘_근무와_다음_근무를_컨텍스트에_담는다() {
        ShiftSchedule night = new ShiftSchedule(
                1L, TODAY, ShiftType.NIGHT, new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        ShiftSchedule next = new ShiftSchedule(
                1L, TODAY.plusDays(2), ShiftType.DAY, new ShiftTime(LocalTime.of(7, 0), LocalTime.of(15, 0)));
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, TODAY)).thenReturn(Optional.of(night));
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, TODAY.plusDays(1), TODAY.plusDays(14)))
                .thenReturn(List.of(next));
        when(transitionDetector.detectTransition(1L, TODAY, ShiftType.NIGHT)).thenReturn(Optional.empty());

        String context = chatContextBuilder.build(1L, TODAY);

        assertTrue(context.contains("NIGHT"));
        assertTrue(context.contains("22:00"));
        assertTrue(context.contains("2026-08-20"));
    }

    @Test
    void 오늘_일정이_없으면_없음을_안내한다() {
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, TODAY)).thenReturn(Optional.empty());
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, TODAY.plusDays(1), TODAY.plusDays(14)))
                .thenReturn(List.of());

        String context = chatContextBuilder.build(1L, TODAY);

        assertTrue(context.contains("등록된 근무 일정이 없습니다"));
    }

    @Test
    void 전환일이면_전환_정보를_담는다() {
        ShiftSchedule day = new ShiftSchedule(
                1L, TODAY, ShiftType.DAY, new ShiftTime(LocalTime.of(7, 0), LocalTime.of(15, 0)));
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, TODAY)).thenReturn(Optional.of(day));
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, TODAY.plusDays(1), TODAY.plusDays(14)))
                .thenReturn(List.of());
        when(transitionDetector.detectTransition(1L, TODAY, ShiftType.DAY))
                .thenReturn(Optional.of(new TransitionDetector.TransitionInfo(ShiftType.NIGHT, ShiftType.DAY)));

        String context = chatContextBuilder.build(1L, TODAY);

        assertTrue(context.contains("전환일"));
    }
}
