package com.midtone.backend.nap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "nap_sessions")
public class NapSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "planned_minutes", nullable = false)
    private Integer plannedMinutes;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expected_end_at", nullable = false)
    private LocalDateTime expectedEndAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NapStatus status;

    protected NapSession() {
    }

    public NapSession(long userId, int plannedMinutes, LocalDateTime startedAt) {
        this.userId = userId;
        this.plannedMinutes = plannedMinutes;
        this.startedAt = startedAt;
        this.expectedEndAt = startedAt.plusMinutes(plannedMinutes);
        this.status = NapStatus.RUNNING;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Integer getPlannedMinutes() { return plannedMinutes; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getExpectedEndAt() { return expectedEndAt; }
    public NapStatus getStatus() { return status; }

    public void finish(NapStatus status, LocalDateTime endedAt) {
        this.status = status;
        this.endedAt = endedAt;
    }
}
