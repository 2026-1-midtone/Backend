package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftScheduleWindow;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private static final int MAX_BULK_RANGE_DAYS = 90;

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final CurrentUserIdProvider currentUserIdProvider;
    private final ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger;
    private final ShiftTimeDefaultService shiftTimeDefaultService;

    public ShiftService(
            ShiftScheduleRepository shiftScheduleRepository,
            CurrentUserIdProvider currentUserIdProvider,
            ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger,
            ShiftTimeDefaultService shiftTimeDefaultService) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.currentUserIdProvider = currentUserIdProvider;
        this.shiftCoachingRegenerationTrigger = shiftCoachingRegenerationTrigger;
        this.shiftTimeDefaultService = shiftTimeDefaultService;
    }

    @Transactional
    public ShiftResponse createShift(CreateShiftRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        LocalDate workDate = LocalDate.parse(request.workDate());
        validateNoDuplicate(userId, workDate);
        ShiftSchedule shift = buildShift(userId, workDate, request);
        shiftScheduleRepository.save(shift);
        return ShiftResponse.from(shift);
    }

    @Transactional(readOnly = true)
    public ShiftListResponse getShifts(GetShiftsRequest request) {
        LocalDate from = LocalDate.parse(request.from());
        LocalDate to = LocalDate.parse(request.to());
        validateRange(from, to);
        long userId = currentUserIdProvider.getCurrentUserId();
        List<ShiftSchedule> shiftsWithScanWindow = shiftScheduleRepository
                .findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(userId, from.minusDays(ShiftScheduleWindow.SCAN_DAYS), to);
        Set<LocalDate> transitionDays = TransitionDetector.transitionDaysOf(shiftsWithScanWindow);
        List<ShiftSchedule> shiftsInRange = shiftsWithScanWindow.stream()
                .filter(shift -> !shift.getWorkDate().isBefore(from))
                .toList();
        return ShiftListResponse.from(shiftsInRange, transitionDays);
    }

    @Transactional
    public UpdateShiftResponse updateShift(Long shiftId, UpdateShiftRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        ShiftSchedule shift = shiftScheduleRepository.findByIdAndUserId(shiftId, userId)
                .orElseThrow(() -> new ShiftException(ShiftException.ErrorCode.SHIFT_NOT_FOUND));
        applyUpdate(shift, request);
        List<String> affectedCoachingDates = shiftCoachingRegenerationTrigger.triggerForSingleUpdate(shift.getWorkDate());
        return UpdateShiftResponse.from(shift, affectedCoachingDates);
    }

    @Transactional
    public void deleteShift(Long shiftId) {
        long userId = currentUserIdProvider.getCurrentUserId();
        ShiftSchedule shift = shiftScheduleRepository.findById(shiftId)
                .orElseThrow(() -> new ShiftException(ShiftException.ErrorCode.SHIFT_NOT_FOUND));
        validateOwnership(shift, userId);
        shiftScheduleRepository.delete(shift);
    }

    @Transactional
    public BulkUpdateShiftResponse bulkUpdateShifts(BulkUpdateShiftRequest request) {
        LocalDate from = LocalDate.parse(request.from());
        LocalDate to = LocalDate.parse(request.to());
        validateBulkRange(from, to);
        ShiftType shiftType = ShiftType.valueOf(request.shiftType());
        long userId = currentUserIdProvider.getCurrentUserId();
        List<ShiftSchedule> shifts =
                shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(userId, from, to);
        shifts.forEach(shift -> shift.update(shiftType, null, null));
        List<String> affectedCoachingDates = shiftCoachingRegenerationTrigger.triggerForRange(from, to);
        return new BulkUpdateShiftResponse(shifts.size(), affectedCoachingDates);
    }

    private void validateNoDuplicate(long userId, LocalDate workDate) {
        if (shiftScheduleRepository.existsByUserIdAndWorkDate(userId, workDate)) {
            throw new ShiftException(ShiftException.ErrorCode.DUPLICATE_SHIFT);
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new ShiftException(ShiftException.ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateOwnership(ShiftSchedule shift, long userId) {
        if (!shift.getUserId().equals(userId)) {
            throw new ShiftException(ShiftException.ErrorCode.SHIFT_ACCESS_DENIED);
        }
    }

    private void validateBulkRange(LocalDate from, LocalDate to) {
        validateRange(from, to);
        if (ChronoUnit.DAYS.between(from, to) > MAX_BULK_RANGE_DAYS) {
            throw new ShiftException(ShiftException.ErrorCode.BULK_UPDATE_RANGE_EXCEEDED);
        }
    }

    private void applyUpdate(ShiftSchedule shift, UpdateShiftRequest request) {
        ShiftType shiftType = request.shiftType() == null ? null : ShiftType.valueOf(request.shiftType());
        shift.update(shiftType, parseTime(request.startTime()), parseTime(request.endTime()));
    }

    private ShiftSchedule buildShift(long userId, LocalDate workDate, CreateShiftRequest request) {
        ShiftType shiftType = ShiftType.valueOf(request.shiftType());
        ShiftTime shiftTime = resolveShiftTime(
                userId, shiftType, parseTime(request.startTime()), parseTime(request.endTime()));
        return new ShiftSchedule(userId, workDate, shiftType, shiftTime);
    }

    /**
     * 시각을 하나라도 생략하면 사용자가 정한 근무 유형별 기본 시각으로 채운다.
     * 시각이 비어 있으면 코칭 카드·다음 근무·야간 영양 타이밍이 전부 계산되지 않기 때문이다.
     */
    private ShiftTime resolveShiftTime(long userId, ShiftType shiftType, LocalTime startTime, LocalTime endTime) {
        if (startTime != null && endTime != null) {
            return new ShiftTime(startTime, endTime);
        }
        ShiftTime fallback = shiftTimeDefaultService.resolve(userId, shiftType);
        return new ShiftTime(
                startTime != null ? startTime : fallback.startTime(),
                endTime != null ? endTime : fallback.endTime());
    }

    private LocalTime parseTime(String time) {
        return time == null ? null : LocalTime.parse(time);
    }
}
