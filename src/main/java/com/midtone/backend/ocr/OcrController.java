package com.midtone.backend.ocr;

import com.midtone.backend.ocr.application.ConfirmOcrJobResponse;
import com.midtone.backend.ocr.application.CreateOcrDraftRequest;
import com.midtone.backend.ocr.application.OcrDraftResponse;
import com.midtone.backend.ocr.application.OcrJobDetailResponse;
import com.midtone.backend.ocr.application.OcrJobResponse;
import com.midtone.backend.ocr.application.OcrJobService;
import com.midtone.backend.ocr.application.UpdateOcrDraftRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class OcrController {

    private final OcrJobService ocrJobService;

    public OcrController(OcrJobService ocrJobService) {
        this.ocrJobService = ocrJobService;
    }

    @PostMapping(value = "/ocr/jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OcrJobResponse> uploadJob(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "month", required = false) String month) {
        OcrJobResponse response = ocrJobService.upload(image, month);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/ocr/jobs/{jobId}")
    public ResponseEntity<OcrJobDetailResponse> getJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(ocrJobService.getJob(jobId));
    }

    @PostMapping("/ocr/jobs/{jobId}/drafts")
    public ResponseEntity<OcrDraftResponse> addDraft(
            @PathVariable Long jobId, @Valid @RequestBody CreateOcrDraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ocrJobService.addDraft(jobId, request));
    }

    @PatchMapping("/ocr/jobs/{jobId}/drafts/{draftId}")
    public ResponseEntity<OcrDraftResponse> updateDraft(
            @PathVariable Long jobId, @PathVariable Long draftId, @RequestBody UpdateOcrDraftRequest request) {
        return ResponseEntity.ok(ocrJobService.updateDraft(jobId, draftId, request));
    }

    @PostMapping("/ocr/jobs/{jobId}:confirm")
    public ResponseEntity<ConfirmOcrJobResponse> confirmJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(ocrJobService.confirm(jobId));
    }

    @PostMapping("/ocr/jobs/{jobId}:retry")
    public ResponseEntity<OcrJobResponse> retryJob(@PathVariable Long jobId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ocrJobService.retry(jobId));
    }
}
