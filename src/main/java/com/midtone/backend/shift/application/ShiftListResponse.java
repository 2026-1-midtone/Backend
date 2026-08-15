package com.midtone.backend.shift.application;

import com.midtone.backend.shift.domain.ShiftSchedule;
import java.util.List;

public record ShiftListResponse(List<ShiftResponse> shifts) {

    public static ShiftListResponse from(List<ShiftSchedule> shifts) {
        List<ShiftResponse> responses = shifts.stream()
                .map(ShiftResponse::from)
                .toList();
        return new ShiftListResponse(responses);
    }
}
