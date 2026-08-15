package com.midtone.backend.shift.domain;

import java.time.LocalTime;

public record ShiftTime(LocalTime startTime, LocalTime endTime) {
}
