package com.midtone.backend.ocr.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.ocr.domain.OcrJobStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class OcrJobStartupCleanerTest {

    @Test
    void 기동_시_PROCESSING_잡을_FAILED로_정리한다() throws Exception {
        OcrJobRepository repository = mock(OcrJobRepository.class);
        OcrJob stuck = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        stuck.markProcessing();
        given(repository.findByStatus(OcrJobStatus.PROCESSING)).willReturn(List.of(stuck));

        new OcrJobStartupCleaner(repository).run(null);

        assertEquals(OcrJobStatus.FAILED, stuck.getStatus());
        assertEquals("서버 재시작으로 분석이 중단되었습니다. 다시 시도해 주세요.", stuck.getErrorMessage());
    }
}
