package com.midtone.backend.shift.application.schedule;

public record ApplyShiftPatternResponse(
        int createdCount, int updatedCount, Long patternId, CompletenessSummary completeness) {

    public static ApplyShiftPatternResponse of(
            int createdCount, int updatedCount, Long patternId, CompletenessResponse completeness) {
        CompletenessSummary summary = new CompletenessSummary(
                completeness.requiredDays(), completeness.confirmedDays(), completeness.remainingDays());
        return new ApplyShiftPatternResponse(createdCount, updatedCount, patternId, summary);
    }

    public record CompletenessSummary(int requiredDays, int confirmedDays, int remainingDays) {
    }
}
