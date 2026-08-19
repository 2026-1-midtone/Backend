package com.midtone.backend.ocr.application;

import com.midtone.backend.ocr.documentai.DocumentAiClient;
import com.midtone.backend.ocr.domain.OcrDraftShift;
import com.midtone.backend.ocr.domain.OcrDraftShiftRepository;
import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import java.time.YearMonth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Component
public class OcrProcessingWorker {

    private static final String UNRECOGNIZED_MESSAGE = "근무표를 인식하지 못했습니다.";
    private static final String CALL_FAILED_MESSAGE = "Document AI 호출에 실패했습니다.";
    private static final Logger log = LoggerFactory.getLogger(OcrProcessingWorker.class);

    private final OcrJobRepository ocrJobRepository;
    private final OcrDraftShiftRepository ocrDraftShiftRepository;
    private final DocumentAiClient documentAiClient;
    private final OcrDraftParser ocrDraftParser;
    private final OcrFallbackExtractor ocrFallbackExtractor;

    public OcrProcessingWorker(
            OcrJobRepository ocrJobRepository,
            OcrDraftShiftRepository ocrDraftShiftRepository,
            DocumentAiClient documentAiClient,
            OcrDraftParser ocrDraftParser,
            OcrFallbackExtractor ocrFallbackExtractor) {
        this.ocrJobRepository = ocrJobRepository;
        this.ocrDraftShiftRepository = ocrDraftShiftRepository;
        this.documentAiClient = documentAiClient;
        this.ocrDraftParser = ocrDraftParser;
        this.ocrFallbackExtractor = ocrFallbackExtractor;
    }

    @Async("ocrExecutor")
    public void processAsync(Long jobId) {
        process(jobId);
    }

    @Transactional
    public void process(Long jobId) {
        OcrJob job = ocrJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("존재하지 않는 OCR 잡을 처리하려 했습니다. jobId={}", jobId);
            return;
        }
        job.markProcessing();
        ocrJobRepository.save(job);
        try {
            JsonNode document = documentAiClient.process(job.getImage(), job.getImageMime());
            List<OcrDraftParser.ParsedDraft> parsed =
                    ocrDraftParser.parse(document, YearMonth.parse(job.getTargetMonth()));
            if (parsed.isEmpty()) {
                parsed = ocrFallbackExtractor.extract(
                        job.getImage(), job.getImageMime(), YearMonth.parse(job.getTargetMonth()));
            }
            if (parsed.isEmpty()) {
                job.markFailed(UNRECOGNIZED_MESSAGE);
            } else {
                ocrDraftShiftRepository.deleteByJobId(jobId);
                ocrDraftShiftRepository.saveAll(parsed.stream()
                        .map(draft -> new OcrDraftShift(
                                jobId, draft.workDate(), draft.shiftType(), draft.confidence(),
                                draft.startTime(), draft.endTime()))
                        .toList());
                job.markCompleted();
            }
        } catch (DocumentAiClient.DocumentAiCallException e) {
            log.error("Document AI 호출 실패. jobId={}", jobId, e);
            job.markFailed(CALL_FAILED_MESSAGE);
        } catch (RuntimeException e) {
            log.error("OCR 처리 중 예상하지 못한 오류. jobId={}", jobId, e);
            job.markFailed(UNRECOGNIZED_MESSAGE);
        }
        ocrJobRepository.save(job);
    }
}
