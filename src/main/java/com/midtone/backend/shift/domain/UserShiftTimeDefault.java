package com.midtone.backend.shift.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "user_shift_time_defaults")
public class UserShiftTimeDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false, length = 20)
    private ShiftType shiftType;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected UserShiftTimeDefault() {
    }

    public UserShiftTimeDefault(long userId, ShiftType shiftType, LocalTime startTime, LocalTime endTime) {
        this.userId = userId;
        this.shiftType = shiftType;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public ShiftType getShiftType() { return shiftType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }

    public ShiftTime toShiftTime() {
        return new ShiftTime(startTime, endTime);
    }
}
