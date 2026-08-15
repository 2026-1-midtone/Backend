package com.midtone.backend.shift.application.pattern;

import com.midtone.backend.shift.domain.ShiftPattern;
import java.util.List;

public record ShiftPatternListResponse(List<ShiftPatternResponse> patterns) {

    public static ShiftPatternListResponse from(List<ShiftPattern> shiftPatterns) {
        List<ShiftPatternResponse> responses = shiftPatterns.stream()
                .map(ShiftPatternResponse::from)
                .toList();
        return new ShiftPatternListResponse(responses);
    }
}
