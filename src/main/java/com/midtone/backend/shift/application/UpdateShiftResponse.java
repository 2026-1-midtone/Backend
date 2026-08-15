package com.midtone.backend.shift.application;

import com.midtone.backend.shift.domain.ShiftSchedule;
import java.util.List;

public record UpdateShiftResponse(
        Long shiftId,
        String workDate,
        String shiftType,
        String startTime,
        String endTime,
        boolean confirmed,
        List<String> affectedCoachingDates
) {

    public static UpdateShiftResponse from(ShiftSchedule shift, List<String> affectedCoachingDates) {
        return new UpdateShiftResponse(
                shift.getId(),
                shift.getWorkDate().toString(),
                shift.getShiftType().name(),
                shift.getStartTime() == null ? null : shift.getStartTime().toString(),
                shift.getEndTime() == null ? null : shift.getEndTime().toString(),
                shift.isConfirmed(),
                affectedCoachingDates
        );
    }
}
