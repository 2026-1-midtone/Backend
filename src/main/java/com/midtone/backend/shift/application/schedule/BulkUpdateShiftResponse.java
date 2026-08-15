package com.midtone.backend.shift.application.schedule;

import java.util.List;

public record BulkUpdateShiftResponse(int updatedCount, List<String> affectedCoachingDates) {}
