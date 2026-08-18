package com.midtone.backend.ocr.application;

public record UpdateOcrDraftRequest(
        String workDate, String shiftType, String startTime, String endTime, Boolean excluded) {
}
