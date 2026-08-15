package com.midtone.backend.shift.application.schedule;

import java.time.LocalDate;
import java.util.List;

public record CompletenessResponse(int requiredDays, int confirmedDays, int remainingDays, List<String> missingDates) {

    public static CompletenessResponse of(int requiredDays, int confirmedDays, List<LocalDate> missingDates) {
        List<String> missingDateStrings = missingDates.stream().map(LocalDate::toString).toList();
        return new CompletenessResponse(requiredDays, confirmedDays, requiredDays - confirmedDays, missingDateStrings);
    }
}
