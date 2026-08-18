package com.midtone.backend.sleep.domain;

import com.midtone.backend.global.time.DateTimeDefaults;
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
@Table(name = "sleep_logs")
public class SleepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "slept_at", nullable = false)
    private LocalDateTime sleptAt;

    @Column(name = "woke_at", nullable = false)
    private LocalDateTime wokeAt;

    @Column(name = "recorded_timezone", nullable = false, length = 64)
    private String recordedTimezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SleepLogSource source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SleepLog() {
    }

    public SleepLog(long userId, LocalDateTime sleptAt, LocalDateTime wokeAt,
                    String recordedTimezone, SleepLogSource source) {
        this.userId = userId;
        this.sleptAt = sleptAt;
        this.wokeAt = wokeAt;
        this.recordedTimezone = recordedTimezone;
        this.source = source;
        this.createdAt = LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE);
        this.updatedAt = this.createdAt;
    }

    public void update(LocalDateTime sleptAt, LocalDateTime wokeAt,
                       String recordedTimezone, SleepLogSource source) {
        this.sleptAt = sleptAt;
        this.wokeAt = wokeAt;
        this.recordedTimezone = recordedTimezone;
        this.source = source;
        this.updatedAt = LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDateTime getSleptAt() { return sleptAt; }
    public LocalDateTime getWokeAt() { return wokeAt; }
    public String getRecordedTimezone() { return recordedTimezone; }
    public SleepLogSource getSource() { return source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
