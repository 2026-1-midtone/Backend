package com.midtone.backend.routine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "routine_tasks")
public class RoutineTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column
    private String tip;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected RoutineTask() {
    }

    public RoutineTask(
            Long userId,
            LocalDate taskDate,
            String sourceType,
            Long sourceId,
            String category,
            String title,
            String tip,
            LocalDateTime windowStart,
            LocalDateTime windowEnd) {
        this.userId = userId;
        this.taskDate = taskDate;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.category = category;
        this.title = title;
        this.tip = tip;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.status = TaskStatus.PENDING;
    }

    public Long getId() { return id; }
    public LocalDateTime getWindowStart() { return windowStart; }
    public LocalDateTime getWindowEnd() { return windowEnd; }
    public Long getSourceId() { return sourceId; }
    public String getSourceType() { return sourceType; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getTip() { return tip; }
    public TaskStatus getStatus() { return status; }
    public Long getUserId() { return userId; }
    public LocalDate getTaskDate() { return taskDate; }

    public void updateStatus(TaskStatus status, LocalDateTime updatedAt) {
        this.status = status;
        this.completedAt = status == TaskStatus.DONE ? updatedAt : null;
    }
}
