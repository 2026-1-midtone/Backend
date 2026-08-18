package com.midtone.backend.ocr.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.midtone.backend.ocr.documentai.DocumentAiClient;
import com.midtone.backend.ocr.domain.OcrDraftShiftRepository;
import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.ocr.domain.OcrJobStatus;
import com.midtone.backend.shift.domain.ShiftType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class OcrProcessingWorkerTest {

    private OcrJobRepository ocrJobRepository;
    private OcrDraftShiftRepository ocrDraftShiftRepository;
    private DocumentAiClient documentAiClient;
    private OcrDraftParser ocrDraftParser;
    private OcrFallbackExtractor ocrFallbackExtractor;
    private OcrProcessingWorker worker;
    private OcrJob job;

    @BeforeEach
    void setUp() {
        ocrJobRepository = mock(OcrJobRepository.class);
        ocrDraftShiftRepository = mock(OcrDraftShiftRepository.class);
        documentAiClient = mock(DocumentAiClient.class);
        ocrDraftParser = mock(OcrDraftParser.class);
        ocrFallbackExtractor = mock(OcrFallbackExtractor.class);
        worker = new OcrProcessingWorker(
                ocrJobRepository, ocrDraftShiftRepository, documentAiClient, ocrDraftParser, ocrFallbackExtractor);
        job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        given(documentAiClient.process(any(), any()))
                .willReturn(new ObjectMapper().readTree("{\"text\":\"\"}"));
        given(ocrFallbackExtractor.extract(any(), any(), any())).willReturn(List.of());
    }

    @Test
    void 초안을_저장하고_COMPLETED로_바꾼다() {
        given(ocrDraftParser.parse(any(), any())).willReturn(List.of(
                new OcrDraftParser.ParsedDraft(LocalDate.of(2026, 8, 1), ShiftType.DAY, new BigDecimal("0.970"))));

        worker.process(10L);

        verify(ocrDraftShiftRepository).deleteByJobId(10L);
        verify(ocrDraftShiftRepository).saveAll(anyList());
        assertEquals(OcrJobStatus.COMPLETED, job.getStatus());
        verify(ocrFallbackExtractor, never()).extract(any(), any(), any());
    }

    @Test
    void Document_AI_초안이_없으면_이미지_fallback_결과를_저장한다() {
        given(ocrDraftParser.parse(any(), any())).willReturn(List.of());
        given(ocrFallbackExtractor.extract(any(), any(), any())).willReturn(List.of(
                new OcrDraftParser.ParsedDraft(
                        LocalDate.of(2026, 8, 1), ShiftType.NIGHT, new BigDecimal("0.500")),
                new OcrDraftParser.ParsedDraft(
                        LocalDate.of(2026, 8, 2), ShiftType.OFF, new BigDecimal("0.500"))));

        worker.process(10L);

        verify(ocrFallbackExtractor).extract(any(), eq("image/png"), eq(YearMonth.of(2026, 8)));
        verify(ocrDraftShiftRepository).saveAll(anyList());
        assertEquals(OcrJobStatus.COMPLETED, job.getStatus());
    }

    @Test
    void 초안이_없으면_FAILED_처리한다() {
        given(ocrDraftParser.parse(any(), any())).willReturn(List.of());

        worker.process(10L);

        assertEquals(OcrJobStatus.FAILED, job.getStatus());
        assertEquals("근무표를 인식하지 못했습니다.", job.getErrorMessage());
    }

    @Test
    void 파싱_시_targetMonth를_YearMonth로_전달한다() {
        given(ocrDraftParser.parse(any(), any())).willReturn(List.of());

        worker.process(10L);

        verify(ocrDraftParser).parse(any(), eq(YearMonth.of(2026, 8)));
    }

    @Test
    void Document_AI_호출이_실패하면_FAILED_처리한다() {
        given(documentAiClient.process(any(), any()))
                .willThrow(new DocumentAiClient.DocumentAiCallException("boom", null));

        worker.process(10L);

        assertEquals(OcrJobStatus.FAILED, job.getStatus());
        assertEquals("Document AI 호출에 실패했습니다.", job.getErrorMessage());
    }
}
