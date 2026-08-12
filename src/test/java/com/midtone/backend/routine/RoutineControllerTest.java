package com.midtone.backend.routine;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.routine.application.RoutineService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoutineController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoutineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoutineService routineService;

    @Test
    void returnsDailyRoutineWithProgress() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 7);
        given(routineService.getRoutines(date)).willReturn(new RoutineService.DailyRoutine(
                date,
                List.of(new RoutineService.RoutineTaskResponse(901L, "COACHING", 303L, "NAP", "20분 파워냅", "근무 전 휴식", "PENDING")),
                new RoutineService.Progress(1, 0, 0, 0.0)));

        mockMvc.perform(get("/api/v1/routines").param("date", "2026-08-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskDate").value("2026-08-07"))
                .andExpect(jsonPath("$.tasks[0].taskId").value(901))
                .andExpect(jsonPath("$.progress.total").value(1));
    }

    @Test
    void marksRoutineTaskDoneAndReturnsUpdatedProgress() throws Exception {
        given(routineService.updateTaskStatus(901L, "DONE"))
                .willReturn(new RoutineService.UpdatedTask(901L, "DONE", new RoutineService.Progress(2, 1, 0, 0.5)));

        mockMvc.perform(patch("/api/v1/routines/tasks/901")
                        .contentType("application/json")
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(901))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.progress.done").value(1));
    }

    @Test
    void returnsDailyRoutineSummary() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 7);
        given(routineService.getSummary(date)).willReturn(new RoutineService.DailySummary(
                date, 2, 1, 1, 0.5, List.of("20분 파워냅"), List.of("기상 후 빛 노출")));

        mockMvc.perform(get("/api/v1/routines/summary").param("date", "2026-08-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryDate").value("2026-08-07"))
                .andExpect(jsonPath("$.doneCount").value(1))
                .andExpect(jsonPath("$.missedTitles[0]").value("기상 후 빛 노출"));
    }
}
