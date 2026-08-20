package com.midtone.backend.routine.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.routine.domain.TaskStatus;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.routine.domain.RoutineTask;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @Mock
    private RoutineTaskRepository routineTaskRepository;

    @Mock
    private RoutineTask routineTask;

    @Test
    void 잘못된_상태값이면_예외를_던진다() {
        RoutineService routineService = new RoutineService(currentUserIdProvider, routineTaskRepository);

        RoutineException exception = assertThrows(RoutineException.class,
                () -> routineService.updateTaskStatus(1L, "INVALID"));
        assertEquals(RoutineException.ErrorCode.INVALID_STATUS, exception.getErrorCode());
    }

    @Test
    void 존재하지_않는_루틴이면_예외를_던진다() {
        given(routineTaskRepository.findById(1L)).willReturn(Optional.empty());

        RoutineService routineService = new RoutineService(currentUserIdProvider, routineTaskRepository);

        RoutineException exception = assertThrows(RoutineException.class,
                () -> routineService.updateTaskStatus(1L, "DONE"));
        assertEquals(RoutineException.ErrorCode.TASK_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 다른_사용자의_루틴이면_예외를_던진다() {
        given(routineTaskRepository.findById(1L)).willReturn(Optional.of(routineTask));
        given(routineTask.getUserId()).willReturn(2L);
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);

        RoutineService routineService = new RoutineService(currentUserIdProvider, routineTaskRepository);

        RoutineException exception = assertThrows(RoutineException.class,
                () -> routineService.updateTaskStatus(1L, "DONE"));
        assertEquals(RoutineException.ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void 지원하지_않는_리포트_기간이면_예외를_던진다() {
        RoutineService routineService = new RoutineService(currentUserIdProvider, routineTaskRepository);

        RoutineException exception = assertThrows(RoutineException.class,
                () -> routineService.getReport("90d"));
        assertEquals(RoutineException.ErrorCode.INVALID_PERIOD, exception.getErrorCode());
    }

    @Test
    void 리포트는_분류별_완료율을_함께_준다() {
        LocalDate today = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        givenTasks(
                task(today, "NAP", TaskStatus.DONE),
                task(today, "NAP", TaskStatus.PENDING),
                task(today, "CAFFEINE_CUTOFF", TaskStatus.DONE));

        RoutineService.RoutineReport report = newService().getReport("7d");

        assertEquals(2, report.byCategory().size());
        RoutineService.CategoryCompletion nap = findCategory(report, "NAP");
        assertEquals(2, nap.total());
        assertEquals(1, nap.done());
        assertEquals(0.5, nap.completionRate());
        assertEquals(1.0, findCategory(report, "CAFFEINE_CUTOFF").completionRate());
    }

    @Test
    void 리포트는_가장_안_지켜진_분류를_알려준다() {
        LocalDate today = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        givenTasks(
                task(today, "NAP", TaskStatus.DONE),
                task(today, "MEAL", TaskStatus.PENDING),
                task(today, "MEAL", TaskStatus.PENDING));

        assertEquals("MEAL", newService().getReport("7d").weakestCategory());
    }

    @Test
    void 기록이_없으면_가장_안_지켜진_분류도_없다() {
        givenTasks();

        RoutineService.RoutineReport report = newService().getReport("7d");

        assertNull(report.weakestCategory());
        assertTrue(report.byCategory().isEmpty());
    }

    @Test
    void 리포트는_기간_안의_모든_날짜를_하루도_빠짐없이_준다() {
        LocalDate today = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        givenTasks(task(today, "NAP", TaskStatus.DONE));

        RoutineService.RoutineReport report = newService().getReport("7d");

        assertEquals(7, report.byDay().size());
        assertEquals(today.minusDays(6), report.byDay().get(0).date());
        assertEquals(today, report.byDay().get(6).date());
    }

    @Test
    void 루틴이_없던_날은_완료율_0으로_준다() {
        LocalDate today = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        givenTasks(task(today, "NAP", TaskStatus.DONE));

        List<RoutineService.DailyCompletion> byDay = newService().getReport("7d").byDay();

        assertEquals(0, byDay.get(0).total());
        assertEquals(0.0, byDay.get(0).completionRate());
        assertEquals(1, byDay.get(6).total());
        assertEquals(1.0, byDay.get(6).completionRate());
    }

    @Test
    void 아무도_손대지_않은_날은_주간_완료율에서_뺀다() {
        LocalDate today = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        // 어제 것은 화면을 열어보느라 만들어졌을 뿐 손댄 적이 없다. 실행하지 못한 날로 깎으면 안 된다.
        givenTasks(
                task(today.minusDays(1), "NAP", TaskStatus.PENDING),
                task(today.minusDays(1), "MEAL", TaskStatus.PENDING),
                task(today, "NAP", TaskStatus.DONE),
                task(today, "MEAL", TaskStatus.DONE));

        assertEquals(1.0, newService().getReport("7d").overallCompletionRate());
    }

    @Test
    void 하루라도_손댄_날은_못한_것까지_모두_센다() {
        LocalDate today = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        givenTasks(
                task(today, "NAP", TaskStatus.DONE),
                task(today, "MEAL", TaskStatus.PENDING),
                task(today, "LIGHT", TaskStatus.PENDING));

        assertEquals(1.0 / 3, newService().getReport("7d").overallCompletionRate());
    }

    @Test
    void 건너뛴_것도_그날을_손댄_것으로_본다() {
        LocalDate today = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        givenTasks(
                task(today, "NAP", TaskStatus.SKIPPED),
                task(today, "MEAL", TaskStatus.PENDING));

        assertEquals(0.0, newService().getReport("7d").overallCompletionRate());
        assertEquals(2, findCategory(newService().getReport("7d"), "NAP").total()
                + findCategory(newService().getReport("7d"), "MEAL").total());
    }

    @Test
    void 분류별_완료율도_손대지_않은_날은_빼고_센다() {
        LocalDate today = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        givenTasks(
                task(today.minusDays(1), "NAP", TaskStatus.PENDING),
                task(today, "NAP", TaskStatus.DONE));

        RoutineService.CategoryCompletion nap = findCategory(newService().getReport("7d"), "NAP");
        assertEquals(1, nap.total());
        assertEquals(1.0, nap.completionRate());
    }

    @Test
    void 날짜별_그래프는_손대지_않은_날도_있는_그대로_준다() {
        LocalDate today = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        givenTasks(
                task(today.minusDays(1), "NAP", TaskStatus.PENDING),
                task(today, "NAP", TaskStatus.DONE));

        List<RoutineService.DailyCompletion> byDay = newService().getReport("7d").byDay();

        assertEquals(1, byDay.get(5).total());
        assertEquals(0, byDay.get(5).done());
        assertEquals(1, byDay.get(6).total());
    }

    private RoutineService newService() {
        return new RoutineService(currentUserIdProvider, routineTaskRepository);
    }

    private void givenTasks(RoutineTask... tasks) {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(routineTaskRepository.findAllByUserIdAndTaskDateBetweenOrderByTaskDateAscIdAsc(anyLong(), any(), any()))
                .willReturn(List.of(tasks));
    }

    private static RoutineTask task(LocalDate date, String category, TaskStatus status) {
        RoutineTask task = new RoutineTask(1L, date, "COACHING", 1L, category, category, null,
                LocalDateTime.of(date, LocalTime.NOON), LocalDateTime.of(date, LocalTime.NOON).plusMinutes(30));
        task.updateStatus(status, LocalDateTime.of(date, LocalTime.NOON));
        return task;
    }

    private static RoutineService.CategoryCompletion findCategory(RoutineService.RoutineReport report, String category) {
        return report.byCategory().stream().filter(item -> item.category().equals(category)).findFirst().orElseThrow();
    }
}
