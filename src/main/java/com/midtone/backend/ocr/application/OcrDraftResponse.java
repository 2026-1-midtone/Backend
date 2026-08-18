package com.midtone.backend.ocr.application;

import com.midtone.backend.ocr.domain.OcrDraftShift;
import java.math.BigDecimal;

public record OcrDraftResponse(
        Long draftId, String workDate, String shiftType,
        String startTime, String endTime, BigDecimal confidence, boolean excluded) {

    public static OcrDraftResponse from(OcrDraftShift draft) {
        return new OcrDraftResponse(
                draft.getId(),
                draft.getWorkDate().toString(),
                draft.getShiftType().name(),
                draft.getStartTime() == null ? null : draft.getStartTime().toString(),
                draft.getEndTime() == null ? null : draft.getEndTime().toString(),
                draft.getConfidence(),
                draft.isExcluded());
    }
}
