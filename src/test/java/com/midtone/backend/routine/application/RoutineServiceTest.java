package com.midtone.backend.routine.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.routine.domain.RoutineTask;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
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
}
