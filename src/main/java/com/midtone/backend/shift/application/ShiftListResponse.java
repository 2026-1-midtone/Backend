package com.midtone.backend.shift.application;

import com.midtone.backend.shift.domain.ShiftSchedule;
import java.util.List;

public record ShiftListResponse(List<ShiftListItemResponse> shifts) {

    public static ShiftListResponse from(List<ShiftSchedule> shifts) {
        List<ShiftListItemResponse> responses = shifts.stream()
                .map(ShiftListItemResponse::from)
                .toList();
        return new ShiftListResponse(responses);
    }
}
