package com.midtone.backend.coaching.application;

import java.util.List;

public record RegenerateCoachingResponse(List<String> regeneratedDates, int regeneratedCount) {}
