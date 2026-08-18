package com.midtone.backend.sleep.application;

import java.util.List;

public record SleepLogListResponse(List<SleepLogResponse> logs) {
}
