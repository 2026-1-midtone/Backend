package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.shift.domain.ShiftSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record ShiftListResponse(List<Item> shifts) {

    public static ShiftListResponse from(List<ShiftSchedule> shifts, Set<LocalDate> transitionDays) {
        List<Item> items = shifts.stream()
                .map(shift -> Item.from(shift, transitionDays.contains(shift.getWorkDate())))
                .toList();
        return new ShiftListResponse(items);
    }

    public record Item(
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

        public static Item from(ShiftSchedule shift, boolean isTransitionDay) {
            return new Item(
                    shift.getId(),
                    shift.getWorkDate().toString(),
                    shift.getShiftType().name(),
                    shift.getStartTime() == null ? null : shift.getStartTime().toString(),
                    shift.getEndTime() == null ? null : shift.getEndTime().toString(),
                    shift.getSource().name(),
                    shift.getConfidence() == null ? null : shift.getConfidence().doubleValue(),
                    shift.isConfirmed(),
                    isTransitionDay
            );
        }
    }
}
