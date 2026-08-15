package com.midtone.backend.coaching.domain;

import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.user.domain.CaffeineSensitivity;
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
@Table(name = "daily_coachings")
public class DailyCoaching {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coaching_date", nullable = false)
    private LocalDate coachingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "today_shift_type", nullable = false, length = 20)
    private ShiftType todayShiftType;

    @Column(name = "next_shift_start_at")
    private LocalDateTime nextShiftStartAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "caffeine_sensitivity_used", nullable = false, length = 20)
    private CaffeineSensitivity caffeineSensitivityUsed;

    @Column(name = "is_transition_day", nullable = false)
    private boolean transitionDay;

    protected DailyCoaching() {
    }

    public DailyCoaching(Long userId, DailyCoachingContent content) {
        this.userId = userId;
        this.coachingDate = content.coachingDate();
        this.todayShiftType = content.todayShiftType();
        this.nextShiftStartAt = content.nextShiftStartAt();
        this.caffeineSensitivityUsed = content.caffeineSensitivityUsed();
        this.transitionDay = content.transitionDay();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getCoachingDate() { return coachingDate; }
    public ShiftType getTodayShiftType() { return todayShiftType; }
    public LocalDateTime getNextShiftStartAt() { return nextShiftStartAt; }
    public CaffeineSensitivity getCaffeineSensitivityUsed() { return caffeineSensitivityUsed; }
    public boolean isTransitionDay() { return transitionDay; }

    public record DailyCoachingContent(
            LocalDate coachingDate,
            ShiftType todayShiftType,
            LocalDateTime nextShiftStartAt,
            CaffeineSensitivity caffeineSensitivityUsed,
            boolean transitionDay) {
    }
}
