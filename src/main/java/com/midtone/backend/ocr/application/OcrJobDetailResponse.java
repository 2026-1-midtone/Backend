package com.midtone.backend.ocr.application;

import com.midtone.backend.ocr.domain.OcrJob;
import java.util.List;

public record OcrJobDetailResponse(
        Long jobId, String status, String targetMonth, String errorMessage, List<OcrDraftResponse> drafts) {

    public static OcrJobDetailResponse from(OcrJob job, List<OcrDraftResponse> drafts) {
        return new OcrJobDetailResponse(
                job.getId(), job.getStatus().name(), job.getTargetMonth(), job.getErrorMessage(), drafts);
    }
}
