package com.midtone.backend.routine.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.midtone.backend.coaching.domain.CoachingCard;
import com.midtone.backend.coaching.domain.CoachingCardType;
import com.midtone.backend.routine.domain.RoutineTask;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
import com.midtone.backend.routine.domain.TaskStatus;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.transition.application.TransitionProtocolCatalog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoutineTaskGeneratorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 10);

    @Mock
    private RoutineTaskRepository routineTaskRepository;
    @Mock
    private TransitionDetector transitionDetector;

    private RoutineTaskGenerator generator() {
        return new RoutineTaskGenerator(routineTaskRepository, transitionDetector, new TransitionProtocolCatalog());
    }

    @Test
    void 코칭_카드를_루틴_태스크로_변환한다() {
        CoachingCard card = new CoachingCard(5L, new CoachingCard.CoachingCardContent(
                CoachingCardType.LIGHT_EXPOSURE,
                "빛 노출",
                LocalDateTime.of(2026, 8, 10, 6, 0),
                LocalDateTime.of(2026, 8, 10, 8, 0),
                "기상 후 밝은 빛을 쬐세요",
                "빛 노출은 리듬 정렬에 도움이 됩니다"));
        ReflectionTestUtils.setField(card, "id", 10L);
        when(transitionDetector.detectTransition(1L, DATE, ShiftType.DAY)).thenReturn(Optional.empty());

        generator().regenerate(1L, DATE, ShiftType.DAY, List.of(card));

        ArgumentCaptor<List<RoutineTask>> captor = ArgumentCaptor.captor();
        InOrder order = inOrder(routineTaskRepository);
        order.verify(routineTaskRepository).deleteAllByUserIdAndTaskDateAndSourceTypeIn(
                eq(1L), eq(DATE), anyCollection());
        order.verify(routineTaskRepository).saveAll(captor.capture());
        List<RoutineTask> tasks = captor.getValue();
        assertEquals(1, tasks.size());
        RoutineTask task = tasks.get(0);
        assertEquals(1L, task.getUserId());
        assertEquals(DATE, task.getTaskDate());
        assertEquals("COACHING", task.getSourceType());
        assertEquals(10L, task.getSourceId());
        assertEquals("LIGHT_EXPOSURE", task.getCategory());
        assertEquals("빛 노출", task.getTitle());
        assertEquals("기상 후 밝은 빛을 쬐세요", task.getTip());
        assertEquals(LocalDateTime.of(2026, 8, 10, 6, 0), task.getWindowStart());
        assertEquals(LocalDateTime.of(2026, 8, 10, 8, 0), task.getWindowEnd());
        assertEquals(TaskStatus.PENDING, task.getStatus());
    }

    @Test
    void 전환일이면_전환_프로토콜_당일_단계를_추가한다() {
        when(transitionDetector.detectTransition(1L, DATE, ShiftType.DAY))
                .thenReturn(Optional.of(new TransitionDetector.TransitionInfo(ShiftType.NIGHT, ShiftType.DAY)));

        generator().regenerate(1L, DATE, ShiftType.DAY, List.of());

        ArgumentCaptor<List<RoutineTask>> captor = ArgumentCaptor.captor();
        org.mockito.Mockito.verify(routineTaskRepository).saveAll(captor.capture());
        List<RoutineTask> tasks = captor.getValue();
        assertEquals(3, tasks.size());
        RoutineTask sleepTask = tasks.get(2);
        assertEquals("TRANSITION", sleepTask.getSourceType());
        assertNull(sleepTask.getSourceId());
        assertEquals("SLEEP", sleepTask.getCategory());
        assertEquals(LocalDateTime.of(2026, 8, 10, 22, 0), sleepTask.getWindowStart());
        assertEquals(LocalDateTime.of(2026, 8, 11, 6, 0), sleepTask.getWindowEnd());
        assertEquals(TaskStatus.PENDING, sleepTask.getStatus());
    }

    @Test
    void 코칭_카드와_전환_단계가_없으면_저장하지_않는다() {
        when(transitionDetector.detectTransition(1L, DATE, ShiftType.OFF)).thenReturn(Optional.empty());

        generator().regenerate(1L, DATE, ShiftType.OFF, List.of());

        org.mockito.Mockito.verify(routineTaskRepository)
                .deleteAllByUserIdAndTaskDateAndSourceTypeIn(eq(1L), eq(DATE), anyCollection());
        org.mockito.Mockito.verify(routineTaskRepository, org.mockito.Mockito.never()).saveAll(anyCollection());
    }
}
