package com.midtone.backend.shift.application.schedule;

import java.util.List;

public record ApplyShiftPatternResponse(
        int createdCount,
        int updatedCount,
        Long patternId,
        CompletenessSummary completeness,
        List<String> affectedCoachingDates) {

    public static ApplyShiftPatternResponse of(
            int createdCount,
            int updatedCount,
            Long patternId,
            CompletenessResponse completeness,
            List<String> affectedCoachingDates) {
        CompletenessSummary summary = new CompletenessSummary(
                completeness.requiredDays(), completeness.confirmedDays(), completeness.remainingDays());
        return new ApplyShiftPatternResponse(createdCount, updatedCount, patternId, summary, affectedCoachingDates);
    }

    public record CompletenessSummary(int requiredDays, int confirmedDays, int remainingDays) {
    }
}
