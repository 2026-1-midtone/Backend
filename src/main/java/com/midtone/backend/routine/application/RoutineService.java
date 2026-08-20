package com.midtone.backend.routine.application;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
import com.midtone.backend.routine.domain.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutineService {

    private static final String PERIOD_7D = "7d";
    private static final String PERIOD_30D = "30d";

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
                format(task.getWindowStart()), format(task.getWindowEnd()), task.getStatus().name())).toList();
        int done = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        int skipped = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.SKIPPED).count();
        int total = tasks.size();
        double completionRate = total == 0 ? 0.0 : (double) done / total;
        return new DailyRoutine(date, mappedTasks, new Progress(total, done, skipped, completionRate));
    }

    @Transactional
    public UpdatedTask updateTaskStatus(long taskId, String requestedStatus) {
        TaskStatus status = parseStatus(requestedStatus);
        com.midtone.backend.routine.domain.RoutineTask task = routineTaskRepository.findById(taskId)
                .orElseThrow(() -> new RoutineException(RoutineException.ErrorCode.TASK_NOT_FOUND));
        if (task.getUserId() != currentUserIdProvider.getCurrentUserId()) {
            throw new RoutineException(RoutineException.ErrorCode.ACCESS_DENIED);
        }
        task.updateStatus(status, LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE));
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

    public RoutineReport getReport(String period) {
        if (!PERIOD_7D.equals(period) && !PERIOD_30D.equals(period)) {
            throw new RoutineException(RoutineException.ErrorCode.INVALID_PERIOD);
        }
        int days = PERIOD_7D.equals(period) ? 7 : 30;
        LocalDate to = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        LocalDate from = to.minusDays(days - 1L);
        List<com.midtone.backend.routine.domain.RoutineTask> tasks = routineTaskRepository
                .findAllByUserIdAndTaskDateBetweenOrderByTaskDateAscIdAsc(currentUserIdProvider.getCurrentUserId(), from, to);
        int done = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        double completionRate = tasks.isEmpty() ? 0.0 : (double) done / tasks.size();
        Set<LocalDate> completedDates = tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE)
                .map(com.midtone.backend.routine.domain.RoutineTask::getTaskDate).collect(Collectors.toSet());
        int current = 0;
        LocalDate cursor = to;
        while (completedDates.contains(cursor)) { current++; cursor = cursor.minusDays(1); }
        int longest = 0;
        int running = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (completedDates.contains(date)) { running++; longest = Math.max(longest, running); } else { running = 0; }
        }
        LocalDate lastCheckinDate = completedDates.stream().max(LocalDate::compareTo).orElse(null);
        List<CategoryCompletion> byCategory = summarizeByCategory(tasks);
        return new RoutineReport(period, from, to, completionRate, byCategory, weakestCategory(byCategory),
                summarizeByDay(tasks, from, to), new Streak(current, longest, lastCheckinDate));
    }

    /**
     * 분류별 완료율. 어떤 습관이 잘 지켜지지 않는지 화면에서 그대로 보여주기 위한 값이다.
     * 순서를 고정해야 화면이 매번 뒤바뀌지 않으므로 분류 이름순으로 정렬한다.
     */
    private static List<CategoryCompletion> summarizeByCategory(
            List<com.midtone.backend.routine.domain.RoutineTask> tasks) {
        return tasks.stream()
                .collect(Collectors.groupingBy(com.midtone.backend.routine.domain.RoutineTask::getCategory,
                        java.util.TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> toCategoryCompletion(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static CategoryCompletion toCategoryCompletion(
            String category, List<com.midtone.backend.routine.domain.RoutineTask> tasks) {
        int done = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        return new CategoryCompletion(category, tasks.size(), done, (double) done / tasks.size());
    }

    /** 완료율이 가장 낮은 분류. 기록이 없으면 지목할 대상도 없다. */
    private static String weakestCategory(List<CategoryCompletion> byCategory) {
        return byCategory.stream()
                .min(java.util.Comparator.comparingDouble(CategoryCompletion::completionRate))
                .map(CategoryCompletion::category)
                .orElse(null);
    }

    /** 기간 안의 모든 날짜를 채워서 준다. 루틴이 없던 날도 그래프에서 빈칸으로 남지 않게 하려는 것이다. */
    private static List<DailyCompletion> summarizeByDay(
            List<com.midtone.backend.routine.domain.RoutineTask> tasks, LocalDate from, LocalDate to) {
        java.util.Map<LocalDate, List<com.midtone.backend.routine.domain.RoutineTask>> tasksByDate = tasks.stream()
                .collect(Collectors.groupingBy(com.midtone.backend.routine.domain.RoutineTask::getTaskDate));
        List<DailyCompletion> byDay = new java.util.ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<com.midtone.backend.routine.domain.RoutineTask> dayTasks = tasksByDate.getOrDefault(date, List.of());
            int done = (int) dayTasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
            byDay.add(new DailyCompletion(date, dayTasks.size(), done,
                    dayTasks.isEmpty() ? 0.0 : (double) done / dayTasks.size()));
        }
        return byDay;
    }

    private TaskStatus parseStatus(String requestedStatus) {
        try {
            return TaskStatus.valueOf(requestedStatus);
        } catch (IllegalArgumentException exception) {
            throw new RoutineException(RoutineException.ErrorCode.INVALID_STATUS);
        }
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(DateTimeDefaults.DEFAULT_ZONE).toOffsetDateTime().toString();
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

    /**
     * @param windowStart 이 할 일을 하기 좋은 시간대의 시작. 코칭 카드와 같은 오프셋 표기를 쓴다.
     */
    public record RoutineTaskResponse(Long taskId, String sourceType, Long sourceId, String category, String title,
                                      String tip, String windowStart, String windowEnd, String status) {
    }

    public record Progress(int total, int done, int skipped, double completionRate) {
    }

    public record UpdatedTask(Long taskId, String status, Progress progress) {
    }

    public record DailySummary(LocalDate summaryDate, int totalCount, int doneCount, int skippedCount,
                               double completionRate, List<String> doneTitles, List<String> missedTitles) {
    }

    /**
     * @param weakestCategory 완료율이 가장 낮은 분류. 기록이 없으면 null이다.
     */
    public record RoutineReport(String period, LocalDate from, LocalDate to, double overallCompletionRate,
                                List<CategoryCompletion> byCategory, String weakestCategory,
                                List<DailyCompletion> byDay, Streak streak) {
    }

    public record CategoryCompletion(String category, int total, int done, double completionRate) {
    }

    public record DailyCompletion(LocalDate date, int total, int done, double completionRate) {
    }

    public record Streak(int currentStreak, int longestStreak, LocalDate lastCheckinDate) {
    }
}
