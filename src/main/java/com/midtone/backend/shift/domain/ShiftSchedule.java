package com.midtone.backend.shift.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shift_schedules")
public class ShiftSchedule {

    private static final ShiftSource DEFAULT_SOURCE = ShiftSource.MANUAL;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false, length = 20)
    private ShiftType shiftType;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftSource source;

    @Column
    private BigDecimal confidence;

    @Column(nullable = false)
    private boolean confirmed;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ShiftSchedule() {
    }

    public ShiftSchedule(Long userId, LocalDate workDate, ShiftType shiftType, ShiftTime shiftTime) {
        this.userId = userId;
        this.workDate = workDate;
        this.shiftType = shiftType;
        this.startTime = shiftTime.startTime();
        this.endTime = shiftTime.endTime();
        this.source = DEFAULT_SOURCE;
        this.confirmed = true;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getWorkDate() { return workDate; }
    public ShiftType getShiftType() { return shiftType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public ShiftSource getSource() { return source; }
    public BigDecimal getConfidence() { return confidence; }
    public boolean isConfirmed() { return confirmed; }
}
