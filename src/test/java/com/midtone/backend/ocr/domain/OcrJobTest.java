package com.midtone.backend.ocr.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OcrJobTest {

    @Test
    void 생성_직후에는_PENDING_상태다() {
        OcrJob job = new OcrJob(1L, new byte[] {1, 2}, "image/png", "2026-08");
        assertEquals(OcrJobStatus.PENDING, job.getStatus());
    }

    @Test
    void 상태_전이가_순서대로_동작한다() {
        OcrJob job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        job.markProcessing();
        assertEquals(OcrJobStatus.PROCESSING, job.getStatus());
        job.markCompleted();
        assertEquals(OcrJobStatus.COMPLETED, job.getStatus());
    }

    @Test
    void 실패_시_사유를_기록한다() {
        OcrJob job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        job.markProcessing();
        job.markFailed("근무표를 인식하지 못했습니다.");
        assertEquals(OcrJobStatus.FAILED, job.getStatus());
        assertEquals("근무표를 인식하지 못했습니다.", job.getErrorMessage());
    }

    @Test
    void 재처리를_시작하면_실패_사유를_비운다() {
        OcrJob job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        job.markProcessing();
        job.markFailed("실패");
        job.markProcessing();
        assertEquals(OcrJobStatus.PROCESSING, job.getStatus());
        assertNull(job.getErrorMessage());
    }

    @Test
    void 확정하면_이미지를_비운다() {
        OcrJob job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        job.markProcessing();
        job.markCompleted();
        job.markConfirmed();
        assertEquals(OcrJobStatus.CONFIRMED, job.getStatus());
        assertNull(job.getImage());
    }

    @Test
    void 소유자를_판별한다() {
        OcrJob job = new OcrJob(7L, new byte[] {1}, "image/png", "2026-08");
        assertTrue(job.isOwnedBy(7L));
        assertFalse(job.isOwnedBy(8L));
    }
}
