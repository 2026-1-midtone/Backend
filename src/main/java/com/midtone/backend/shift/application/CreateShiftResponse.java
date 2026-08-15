package com.midtone.backend.shift.application;

import com.midtone.backend.shift.domain.ShiftSchedule;

public record CreateShiftResponse(
        Long shiftId,
        String workDate,
        String shiftType,
        String startTime,
        String endTime,
        String source,
        boolean confirmed
) {

    public static CreateShiftResponse from(ShiftSchedule shift) {
        return new CreateShiftResponse(
                shift.getId(),
                shift.getWorkDate().toString(),
                shift.getShiftType().name(),
                shift.getStartTime() == null ? null : shift.getStartTime().toString(),
                shift.getEndTime() == null ? null : shift.getEndTime().toString(),
                shift.getSource().name(),
                shift.isConfirmed()
        );
    }
}
