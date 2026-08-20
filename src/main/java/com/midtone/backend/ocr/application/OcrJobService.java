package com.midtone.backend.ocr.application;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.ocr.domain.OcrDraftShift;
import com.midtone.backend.ocr.domain.OcrDraftShiftRepository;
import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.ocr.domain.OcrJobStatus;
import com.midtone.backend.shift.application.schedule.ShiftCoachingRegenerationTrigger;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrJobService {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final OcrJobRepository ocrJobRepository;
    private final OcrDraftShiftRepository ocrDraftShiftRepository;
    private final ShiftScheduleRepository shiftScheduleRepository;
    private final OcrProcessingWorker ocrProcessingWorker;
    private final ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger;
    private final CurrentUserIdProvider currentUserIdProvider;

    public OcrJobService(
            OcrJobRepository ocrJobRepository,
            OcrDraftShiftRepository ocrDraftShiftRepository,
            ShiftScheduleRepository shiftScheduleRepository,
            OcrProcessingWorker ocrProcessingWorker,
            ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger,
            CurrentUserIdProvider currentUserIdProvider) {
        this.ocrJobRepository = ocrJobRepository;
        this.ocrDraftShiftRepository = ocrDraftShiftRepository;
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.ocrProcessingWorker = ocrProcessingWorker;
        this.shiftCoachingRegenerationTrigger = shiftCoachingRegenerationTrigger;
        this.currentUserIdProvider = currentUserIdProvider;
    }

    public OcrJobResponse upload(MultipartFile image, String month) {
        long userId = currentUserIdProvider.getCurrentUserId();
        validateImage(image);
        String targetMonth = resolveMonth(month);
        OcrJob job = ocrJobRepository.save(new OcrJob(userId, readBytes(image), image.getContentType(), targetMonth));
        ocrProcessingWorker.processAsync(job.getId());
        return new OcrJobResponse(job.getId(), job.getStatus().name());
    }

    @Transactional(readOnly = true)
    public OcrJobDetailResponse getJob(Long jobId) {
        OcrJob job = findOwnedJob(jobId);
        List<OcrDraftResponse> drafts = job.getStatus() == OcrJobStatus.COMPLETED
                ? ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(jobId).stream()
                        .map(OcrDraftResponse::from)
                        .toList()
                : List.of();
        return OcrJobDetailResponse.from(job, drafts);
    }

    @Transactional
    public OcrDraftResponse updateDraft(Long jobId, Long draftId, UpdateOcrDraftRequest request) {
        OcrJob job = findOwnedJob(jobId);
        requireCompleted(job);
        OcrDraftShift draft = ocrDraftShiftRepository.findById(draftId)
                .filter(found -> found.getJobId().equals(jobId))
                .orElseThrow(() -> new OcrException(OcrException.ErrorCode.DRAFT_NOT_FOUND));
        draft.applyCorrection(
                request.workDate() == null ? null : LocalDate.parse(request.workDate()),
                request.shiftType() == null ? null : ShiftType.valueOf(request.shiftType()),
                request.startTime() == null ? null : LocalTime.parse(request.startTime()),
                request.endTime() == null ? null : LocalTime.parse(request.endTime()),
                request.excluded());
        return OcrDraftResponse.from(draft);
    }

    @Transactional
    public ConfirmOcrJobResponse confirm(Long jobId) {
        OcrJob job = findOwnedJob(jobId);
        requireCompleted(job);
        long userId = job.getUserId();
        List<OcrDraftShift> candidates = ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(jobId).stream()
                .filter(draft -> !draft.isExcluded())
                .toList();
        List<String> skippedDates = new ArrayList<>();
        List<OcrDraftShift> drafts = keepBestDraftPerDate(candidates, skippedDates);
        List<String> replacedDates = new ArrayList<>();
        List<ShiftSchedule> newShifts = new ArrayList<>();
        for (OcrDraftShift draft : drafts) {
            shiftScheduleRepository.findByUserIdAndWorkDate(userId, draft.getWorkDate()).ifPresent(existing -> {
                shiftScheduleRepository.delete(existing);
                replacedDates.add(draft.getWorkDate().toString());
            });
            newShifts.add(ShiftSchedule.fromOcr(
                    userId, draft.getWorkDate(), draft.getShiftType(),
                    new ShiftTime(draft.getStartTime(), draft.getEndTime()), draft.getConfidence()));
        }
        shiftScheduleRepository.flush();
        shiftScheduleRepository.saveAll(newShifts);
        List<String> affectedCoachingDates = List.of();
        if (!drafts.isEmpty()) {
            affectedCoachingDates = shiftCoachingRegenerationTrigger.triggerForRange(
                    drafts.get(0).getWorkDate(), drafts.get(drafts.size() - 1).getWorkDate());
        }
        job.markConfirmed();
        ocrJobRepository.save(job);
        return new ConfirmOcrJobResponse(newShifts.size(), replacedDates, affectedCoachingDates, skippedDates);
    }

    @Transactional
    public OcrJobResponse retry(Long jobId) {
        OcrJob job = findOwnedJob(jobId);
        if (job.getStatus() != OcrJobStatus.FAILED && job.getStatus() != OcrJobStatus.COMPLETED) {
            throw new OcrException(OcrException.ErrorCode.JOB_NOT_RETRYABLE);
        }
        ocrProcessingWorker.processAsync(jobId);
        return new OcrJobResponse(jobId, OcrJobStatus.PROCESSING.name());
    }

    /**
     * 근무표는 하루에 한 건만 저장할 수 있으므로(uk_shift_schedules_user_date), 같은 날짜에 초안이 여러 개
     * 남아 있으면 신뢰도가 가장 높은 하나만 남긴다. 신뢰도가 같으면 먼저 인식된 초안을 쓴다.
     * 이때 밀려난 날짜는 skippedDates 로 알려 사용자가 검수 화면에서 바로잡을 수 있게 한다.
     */
    private List<OcrDraftShift> keepBestDraftPerDate(List<OcrDraftShift> candidates, List<String> skippedDates) {
        Map<LocalDate, OcrDraftShift> bestByDate = new LinkedHashMap<>();
        for (OcrDraftShift draft : candidates) {
            OcrDraftShift previous = bestByDate.putIfAbsent(draft.getWorkDate(), draft);
            if (previous == null) {
                continue;
            }
            skippedDates.add(draft.getWorkDate().toString());
            if (confidenceOf(draft).compareTo(confidenceOf(previous)) > 0) {
                bestByDate.put(draft.getWorkDate(), draft);
            }
        }
        return List.copyOf(bestByDate.values());
    }

    private BigDecimal confidenceOf(OcrDraftShift draft) {
        return draft.getConfidence() == null ? BigDecimal.ZERO : draft.getConfidence();
    }

    private OcrJob findOwnedJob(Long jobId) {
        OcrJob job = ocrJobRepository.findById(jobId)
                .orElseThrow(() -> new OcrException(OcrException.ErrorCode.JOB_NOT_FOUND));
        if (!job.isOwnedBy(currentUserIdProvider.getCurrentUserId())) {
            throw new OcrException(OcrException.ErrorCode.JOB_ACCESS_DENIED);
        }
        return job;
    }

    private void requireCompleted(OcrJob job) {
        if (job.getStatus() != OcrJobStatus.COMPLETED) {
            throw new OcrException(OcrException.ErrorCode.JOB_NOT_COMPLETED);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new OcrException(OcrException.ErrorCode.IMAGE_REQUIRED);
        }
        if (!SUPPORTED_MIME_TYPES.contains(image.getContentType())) {
            throw new OcrException(OcrException.ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new OcrException(OcrException.ErrorCode.IMAGE_TOO_LARGE);
        }
    }

    private String resolveMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(DateTimeDefaults.DEFAULT_ZONE).toString();
        }
        try {
            return YearMonth.parse(month).toString();
        } catch (DateTimeParseException e) {
            throw new OcrException(OcrException.ErrorCode.INVALID_MONTH);
        }
    }

    private byte[] readBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new OcrException(OcrException.ErrorCode.IMAGE_REQUIRED);
        }
    }
}
