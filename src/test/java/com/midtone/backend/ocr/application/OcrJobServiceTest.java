package com.midtone.backend.ocr.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.ocr.domain.OcrDraftShift;
import com.midtone.backend.ocr.domain.OcrDraftShiftRepository;
import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.ocr.domain.OcrJobStatus;
import com.midtone.backend.shift.application.schedule.ShiftCoachingRegenerationTrigger;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftSource;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class OcrJobServiceTest {

    private OcrJobRepository ocrJobRepository;
    private OcrDraftShiftRepository ocrDraftShiftRepository;
    private ShiftScheduleRepository shiftScheduleRepository;
    private OcrProcessingWorker worker;
    private ShiftCoachingRegenerationTrigger coachingTrigger;
    private OcrJobService service;

    @BeforeEach
    void setUp() {
        ocrJobRepository = mock(OcrJobRepository.class);
        ocrDraftShiftRepository = mock(OcrDraftShiftRepository.class);
        shiftScheduleRepository = mock(ShiftScheduleRepository.class);
        worker = mock(OcrProcessingWorker.class);
        coachingTrigger = mock(ShiftCoachingRegenerationTrigger.class);
        CurrentUserIdProvider userIdProvider = () -> 1L;
        service = new OcrJobService(
                ocrJobRepository, ocrDraftShiftRepository, shiftScheduleRepository,
                worker, coachingTrigger, userIdProvider);
        given(ocrJobRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
    }

    private OcrJob completedJob() {
        OcrJob job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        job.markProcessing();
        job.markCompleted();
        return job;
    }

    @Test
    void 업로드하면_잡을_저장하고_비동기_처리를_시작한다() {
        MockMultipartFile image = new MockMultipartFile("image", "roster.png", "image/png", new byte[] {1, 2});

        OcrJobResponse response = service.upload(image, "2026-08");

        assertEquals("PENDING", response.status());
        verify(worker).processAsync(any());
    }

    @Test
    void month가_없으면_현재_달을_사용한다() {
        MockMultipartFile image = new MockMultipartFile("image", "roster.png", "image/png", new byte[] {1});

        service.upload(image, null);

        org.mockito.ArgumentCaptor<OcrJob> captor = org.mockito.ArgumentCaptor.forClass(OcrJob.class);
        verify(ocrJobRepository).save(captor.capture());
        assertTrue(captor.getValue().getTargetMonth().matches("\\d{4}-\\d{2}"));
    }

    @Test
    void 빈_이미지면_400_예외를_던진다() {
        MockMultipartFile image = new MockMultipartFile("image", "roster.png", "image/png", new byte[] {});
        OcrException exception = assertThrows(OcrException.class, () -> service.upload(image, "2026-08"));
        assertEquals(OcrException.ErrorCode.IMAGE_REQUIRED, exception.getErrorCode());
    }

    @Test
    void 지원하지_않는_이미지_형식이면_400_예외를_던진다() {
        MockMultipartFile image = new MockMultipartFile("image", "roster.gif", "image/gif", new byte[] {1});
        OcrException exception = assertThrows(OcrException.class, () -> service.upload(image, "2026-08"));
        assertEquals(OcrException.ErrorCode.UNSUPPORTED_IMAGE_TYPE, exception.getErrorCode());
    }

    @Test
    void month가_형식에_안_맞으면_400_예외를_던진다() {
        MockMultipartFile image = new MockMultipartFile("image", "roster.png", "image/png", new byte[] {1});
        OcrException exception = assertThrows(OcrException.class, () -> service.upload(image, "2026/08"));
        assertEquals(OcrException.ErrorCode.INVALID_MONTH, exception.getErrorCode());
    }

    @Test
    void 없는_잡을_조회하면_404_예외를_던진다() {
        given(ocrJobRepository.findById(anyLong())).willReturn(Optional.empty());
        OcrException exception = assertThrows(OcrException.class, () -> service.getJob(99L));
        assertEquals(OcrException.ErrorCode.JOB_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 다른_사용자의_잡을_조회하면_403_예외를_던진다() {
        OcrJob othersJob = new OcrJob(2L, new byte[] {1}, "image/png", "2026-08");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(othersJob));
        OcrException exception = assertThrows(OcrException.class, () -> service.getJob(10L));
        assertEquals(OcrException.ErrorCode.JOB_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void COMPLETED_잡을_조회하면_초안_목록을_포함한다() {
        OcrJob job = completedJob();
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        given(ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(10L)).willReturn(List.of(
                new OcrDraftShift(10L, LocalDate.of(2026, 8, 1), ShiftType.DAY, new BigDecimal("0.970"))));

        OcrJobDetailResponse response = service.getJob(10L);

        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.drafts().size());
        assertEquals("2026-08-01", response.drafts().get(0).workDate());
    }

    @Test
    void COMPLETED가_아니면_초안_목록은_비어_있다() {
        OcrJob pending = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(pending));

        OcrJobDetailResponse response = service.getJob(10L);

        assertEquals("PENDING", response.status());
        assertEquals(0, response.drafts().size());
    }

    @Test
    void COMPLETED가_아니면_보정_시_409_예외를_던진다() {
        OcrJob pending = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(pending));
        OcrException exception = assertThrows(OcrException.class,
                () -> service.updateDraft(10L, 5L, new UpdateOcrDraftRequest(null, "NIGHT", null, null, null)));
        assertEquals(OcrException.ErrorCode.JOB_NOT_COMPLETED, exception.getErrorCode());
    }

    @Test
    void 다른_잡의_초안이면_404_예외를_던진다() {
        OcrJob job = completedJob();
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        OcrDraftShift otherJobDraft = new OcrDraftShift(99L, LocalDate.of(2026, 8, 1), ShiftType.DAY, null);
        given(ocrDraftShiftRepository.findById(5L)).willReturn(Optional.of(otherJobDraft));
        OcrException exception = assertThrows(OcrException.class,
                () -> service.updateDraft(10L, 5L, new UpdateOcrDraftRequest(null, "NIGHT", null, null, null)));
        assertEquals(OcrException.ErrorCode.DRAFT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 초안을_보정하면_수정된_값을_반환한다() {
        OcrJob job = completedJob();
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        OcrDraftShift draft = new OcrDraftShift(10L, LocalDate.of(2026, 8, 1), ShiftType.DAY, new BigDecimal("0.970"));
        given(ocrDraftShiftRepository.findById(5L)).willReturn(Optional.of(draft));

        OcrDraftResponse response = service.updateDraft(
                10L, 5L, new UpdateOcrDraftRequest(null, "NIGHT", "22:00", "07:00", null));

        assertEquals("NIGHT", response.shiftType());
        assertEquals("22:00", response.startTime());
    }

    @Test
    void COMPLETED가_아니면_확정_시_409_예외를_던진다() {
        OcrJob pending = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(pending));
        OcrException exception = assertThrows(OcrException.class, () -> service.confirm(10L));
        assertEquals(OcrException.ErrorCode.JOB_NOT_COMPLETED, exception.getErrorCode());
    }

    @Test
    void 확정하면_초안을_일정으로_반영하고_겹치는_날짜는_덮어쓴다() {
        OcrJob job = completedJob();
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        OcrDraftShift draft = new OcrDraftShift(10L, LocalDate.of(2026, 8, 1), ShiftType.DAY, new BigDecimal("0.970"));
        given(ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(10L)).willReturn(List.of(draft));
        ShiftSchedule existing = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 1), ShiftType.NIGHT, new ShiftTime(null, null));
        given(shiftScheduleRepository.findByUserIdAndWorkDate(1L, LocalDate.of(2026, 8, 1)))
                .willReturn(Optional.of(existing));
        given(coachingTrigger.triggerForRange(any(), any())).willReturn(List.of("2026-08-01"));

        ConfirmOcrJobResponse response = service.confirm(10L);

        assertEquals(1, response.confirmedCount());
        assertEquals(List.of("2026-08-01"), response.replacedDates());
        assertEquals(List.of("2026-08-01"), response.affectedCoachingDates());
        verify(shiftScheduleRepository).delete(existing);
        verify(shiftScheduleRepository).saveAll(anyIterable());
        assertEquals(OcrJobStatus.CONFIRMED, job.getStatus());
    }

    @Test
    void 확정된_일정은_OCR_출처와_confidence를_가진다() {
        OcrJob job = completedJob();
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        OcrDraftShift draft = new OcrDraftShift(10L, LocalDate.of(2026, 8, 1), ShiftType.DAY, new BigDecimal("0.970"));
        given(ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(10L)).willReturn(List.of(draft));
        given(shiftScheduleRepository.findByUserIdAndWorkDate(anyLong(), any())).willReturn(Optional.empty());
        given(coachingTrigger.triggerForRange(any(), any())).willReturn(List.of());

        service.confirm(10L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<ShiftSchedule>> captor =
                org.mockito.ArgumentCaptor.forClass((Class) List.class);
        verify(shiftScheduleRepository).saveAll(captor.capture());
        ShiftSchedule saved = captor.getValue().get(0);
        assertEquals(ShiftSource.OCR, saved.getSource());
        assertEquals(new BigDecimal("0.970"), saved.getConfidence());
        assertTrue(saved.isConfirmed());
    }

    @Test
    void 같은_날짜에_초안이_두_개면_신뢰도가_높은_초안만_저장한다() {
        OcrJob job = completedJob();
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        OcrDraftShift off = new OcrDraftShift(10L, LocalDate.of(2026, 8, 4), ShiftType.OFF, new BigDecimal("0.960"));
        OcrDraftShift night =
                new OcrDraftShift(10L, LocalDate.of(2026, 8, 4), ShiftType.NIGHT, new BigDecimal("0.970"));
        given(ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(10L)).willReturn(List.of(off, night));
        given(shiftScheduleRepository.findByUserIdAndWorkDate(anyLong(), any())).willReturn(Optional.empty());
        given(coachingTrigger.triggerForRange(any(), any())).willReturn(List.of());

        ConfirmOcrJobResponse response = service.confirm(10L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<ShiftSchedule>> captor =
                org.mockito.ArgumentCaptor.forClass((Class) List.class);
        verify(shiftScheduleRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(ShiftType.NIGHT, captor.getValue().get(0).getShiftType());
        assertEquals(1, response.confirmedCount());
        assertEquals(List.of("2026-08-04"), response.skippedDates());
    }

    @Test
    void 같은_날짜의_중복_초안이_excluded면_충돌로_보지_않는다() {
        OcrJob job = completedJob();
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        OcrDraftShift off = new OcrDraftShift(10L, LocalDate.of(2026, 8, 4), ShiftType.OFF, new BigDecimal("0.960"));
        off.applyCorrection(null, null, null, null, true);
        OcrDraftShift night =
                new OcrDraftShift(10L, LocalDate.of(2026, 8, 4), ShiftType.NIGHT, new BigDecimal("0.970"));
        given(ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(10L)).willReturn(List.of(off, night));
        given(shiftScheduleRepository.findByUserIdAndWorkDate(anyLong(), any())).willReturn(Optional.empty());
        given(coachingTrigger.triggerForRange(any(), any())).willReturn(List.of());

        ConfirmOcrJobResponse response = service.confirm(10L);

        assertEquals(1, response.confirmedCount());
    }

    @Test
    void excluded_초안은_확정에서_제외한다() {
        OcrJob job = completedJob();
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        OcrDraftShift draft = new OcrDraftShift(10L, LocalDate.of(2026, 8, 1), ShiftType.DAY, null);
        draft.applyCorrection(null, null, null, null, true);
        given(ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(10L)).willReturn(List.of(draft));

        ConfirmOcrJobResponse response = service.confirm(10L);

        assertEquals(0, response.confirmedCount());
        assertEquals(0, response.affectedCoachingDates().size());
    }

    @Test
    void PENDING_잡은_재시도할_수_없다() {
        OcrJob pending = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(pending));
        OcrException exception = assertThrows(OcrException.class, () -> service.retry(10L));
        assertEquals(OcrException.ErrorCode.JOB_NOT_RETRYABLE, exception.getErrorCode());
    }

    @Test
    void FAILED_잡을_재시도하면_다시_처리를_시작한다() {
        OcrJob failed = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        failed.markProcessing();
        failed.markFailed("실패");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(failed));

        OcrJobResponse response = service.retry(10L);

        assertEquals("PROCESSING", response.status());
        verify(worker).processAsync(10L);
    }
}
