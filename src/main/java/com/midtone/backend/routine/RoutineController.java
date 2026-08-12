package com.midtone.backend.routine;

import com.midtone.backend.routine.application.RoutineService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routines")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @GetMapping
    public RoutineService.DailyRoutine getRoutines(@RequestParam(required = false) LocalDate date) {
        return routineService.getRoutines(date == null ? LocalDate.now() : date);
    }

    @GetMapping("/summary")
    public RoutineService.DailySummary getSummary(@RequestParam(required = false) LocalDate date) {
        return routineService.getSummary(date == null ? LocalDate.now() : date);
    }

    @PatchMapping("/tasks/{taskId}")
    public RoutineService.UpdatedTask updateTaskStatus(@PathVariable long taskId, @RequestBody UpdateTaskRequest request) {
        return routineService.updateTaskStatus(taskId, request.status());
    }

    public record UpdateTaskRequest(String status) {
    }
}
