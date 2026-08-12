package com.midtone.backend.routine.application;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
import com.midtone.backend.routine.domain.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoutineService {

    private final CurrentUserIdProvider currentUserIdProvider;
    private final RoutineTaskRepository routineTaskRepository;

    public RoutineService(CurrentUserIdProvider currentUserIdProvider, RoutineTaskRepository routineTaskRepository) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.routineTaskRepository = routineTaskRepository;
    }

    public DailyRoutine getRoutines(LocalDate date) {
        List<com.midtone.backend.routine.domain.RoutineTask> tasks = findTasks(date);
        List<RoutineTaskResponse> mappedTasks = tasks.stream().map(task -> new RoutineTaskResponse(
                task.getId(), task.getSourceType(), task.getSourceId(), task.getCategory(), task.getTitle(), task.getTip(),
                task.getStatus().name())).toList();
        int done = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        int skipped = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.SKIPPED).count();
        int total = tasks.size();
        double completionRate = total == 0 ? 0.0 : (double) done / total;
        return new DailyRoutine(date, mappedTasks, new Progress(total, done, skipped, completionRate));
    }

    public UpdatedTask updateTaskStatus(long taskId, String requestedStatus) {
        TaskStatus status;
        try {
            status = TaskStatus.valueOf(requestedStatus);
        } catch (IllegalArgumentException exception) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "루틴 상태는 PENDING, DONE, SKIPPED 중 하나여야 합니다.");
        }

        com.midtone.backend.routine.domain.RoutineTask task = routineTaskRepository.findById(taskId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "해당 루틴 항목을 찾을 수 없습니다."));
        if (task.getUserId() != currentUserIdProvider.getCurrentUserId()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "접근 권한이 없습니다.");
        }
        task.updateStatus(status, LocalDateTime.now());
        return new UpdatedTask(task.getId(), status.name(), progressFor(task.getTaskDate()));
    }

    public DailySummary getSummary(LocalDate date) {
        List<com.midtone.backend.routine.domain.RoutineTask> tasks = findTasks(date);
        int done = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        int skipped = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.SKIPPED).count();
        int total = tasks.size();
        List<String> doneTitles = tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE)
                .map(com.midtone.backend.routine.domain.RoutineTask::getTitle).toList();
        List<String> missedTitles = tasks.stream().filter(task -> task.getStatus() != TaskStatus.DONE)
                .map(com.midtone.backend.routine.domain.RoutineTask::getTitle).toList();
        return new DailySummary(date, total, done, skipped, total == 0 ? 0.0 : (double) done / total, doneTitles, missedTitles);
    }

    private List<com.midtone.backend.routine.domain.RoutineTask> findTasks(LocalDate date) {
        return routineTaskRepository.findAllByUserIdAndTaskDateOrderByIdAsc(currentUserIdProvider.getCurrentUserId(), date);
    }

    private Progress progressFor(LocalDate date) {
        List<com.midtone.backend.routine.domain.RoutineTask> tasks = findTasks(date);
        int done = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        int skipped = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.SKIPPED).count();
        int total = tasks.size();
        return new Progress(total, done, skipped, total == 0 ? 0.0 : (double) done / total);
    }

    public record DailyRoutine(LocalDate taskDate, List<RoutineTaskResponse> tasks, Progress progress) {
    }

    public record RoutineTaskResponse(Long taskId, String sourceType, Long sourceId, String category, String title,
                                      String tip, String status) {
    }

    public record Progress(int total, int done, int skipped, double completionRate) {
    }

    public record UpdatedTask(Long taskId, String status, Progress progress) {
    }

    public record DailySummary(LocalDate summaryDate, int totalCount, int doneCount, int skippedCount,
                               double completionRate, List<String> doneTitles, List<String> missedTitles) {
    }
}
