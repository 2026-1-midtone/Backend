package com.midtone.backend.shift.application;

import com.midtone.backend.shift.domain.ShiftSchedule;

public record ShiftListItemResponse(
        Long shiftId,
        String workDate,
        String shiftType,
        String startTime,
        String endTime,
        String source,
        Double confidence,
        boolean confirmed,
        boolean isTransitionDay
) {

    private static final boolean TRANSITION_DAY_PLACEHOLDER = false;

    public static ShiftListItemResponse from(ShiftSchedule shift) {
        return new ShiftListItemResponse(
                shift.getId(),
                shift.getWorkDate().toString(),
                shift.getShiftType().name(),
                shift.getStartTime() == null ? null : shift.getStartTime().toString(),
                shift.getEndTime() == null ? null : shift.getEndTime().toString(),
                shift.getSource().name(),
                shift.getConfidence() == null ? null : shift.getConfidence().doubleValue(),
                shift.isConfirmed(),
                TRANSITION_DAY_PLACEHOLDER
        );
    }
}
