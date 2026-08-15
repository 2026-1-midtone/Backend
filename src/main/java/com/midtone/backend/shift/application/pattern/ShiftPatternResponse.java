package com.midtone.backend.shift.application.pattern;

import com.midtone.backend.shift.domain.ShiftPattern;
import java.util.List;

public record ShiftPatternResponse(Long patternId, String name, int cycleDays, List<String> pattern) {

    public static ShiftPatternResponse from(ShiftPattern shiftPattern) {
        List<String> patternTypes = shiftPattern.getPattern().stream()
                .map(Enum::name)
                .toList();
        return new ShiftPatternResponse(
                shiftPattern.getId(), shiftPattern.getName(), shiftPattern.getCycleDays(), patternTypes);
    }
}
