package com.midtone.backend.ocr.application;

import java.util.List;

public record ConfirmOcrJobResponse(
        int confirmedCount, List<String> replacedDates, List<String> affectedCoachingDates) {
}
