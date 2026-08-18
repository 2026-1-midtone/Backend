package com.midtone.backend.ocr.domain;

import com.midtone.backend.shift.domain.ShiftType;
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
import java.time.LocalTime;

@Entity
@Table(name = "ocr_draft_shifts")
public class OcrDraftShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false, length = 20)
    private ShiftType shiftType;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column
    private BigDecimal confidence;

    @Column(nullable = false)
    private boolean excluded;

    protected OcrDraftShift() {
    }

    public OcrDraftShift(Long jobId, LocalDate workDate, ShiftType shiftType, BigDecimal confidence) {
        this.jobId = jobId;
        this.workDate = workDate;
        this.shiftType = shiftType;
        this.confidence = confidence;
        this.excluded = false;
    }

    public void applyCorrection(
            LocalDate workDate, ShiftType shiftType, LocalTime startTime, LocalTime endTime, Boolean excluded) {
        if (workDate != null) {
            this.workDate = workDate;
        }
        if (shiftType != null) {
            this.shiftType = shiftType;
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
        if (excluded != null) {
            this.excluded = excluded;
        }
    }

    public Long getId() { return id; }
    public Long getJobId() { return jobId; }
    public LocalDate getWorkDate() { return workDate; }
    public ShiftType getShiftType() { return shiftType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public BigDecimal getConfidence() { return confidence; }
    public boolean isExcluded() { return excluded; }
}
