package com.midtone.backend.sleep.application;

import java.time.LocalDate;
import java.time.LocalTime;

public record SleepPattern(
        LocalTime habitualBedtime,
        LocalTime habitualWakeTime,
        LocalTime habitualMidSleep,
        LocalTime estimatedCbtMin,
        int sampleCount,
        LocalDate windowFrom,
        LocalDate windowTo) {
}
