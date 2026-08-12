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

    public Long getId() { return id; }
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
