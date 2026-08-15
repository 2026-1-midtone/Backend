package com.midtone.backend.shift.application;

import com.midtone.backend.shift.domain.ShiftSchedule;
import java.util.List;

public record ShiftListResponse(List<Item> shifts) {

    public static ShiftListResponse from(List<ShiftSchedule> shifts) {
        List<Item> items = shifts.stream()
                .map(Item::from)
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

        private static final boolean TRANSITION_DAY_PLACEHOLDER = false;

        public static Item from(ShiftSchedule shift) {
            return new Item(
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
}
