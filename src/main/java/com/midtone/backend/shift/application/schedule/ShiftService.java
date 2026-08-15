package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private static final int MAX_BULK_RANGE_DAYS = 90;

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public ShiftService(
            ShiftScheduleRepository shiftScheduleRepository, CurrentUserIdProvider currentUserIdProvider) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.currentUserIdProvider = currentUserIdProvider;
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
        List<ShiftSchedule> shifts =
                shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(userId, from, to);
        return ShiftListResponse.from(shifts);
    }

    @Transactional
    public UpdateShiftResponse updateShift(Long shiftId, UpdateShiftRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        ShiftSchedule shift = shiftScheduleRepository.findByIdAndUserId(shiftId, userId)
                .orElseThrow(() -> new ShiftException(ShiftException.ErrorCode.SHIFT_NOT_FOUND));
        applyUpdate(shift, request);
        return UpdateShiftResponse.from(shift, List.of());
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
        return new BulkUpdateShiftResponse(shifts.size(), List.of());
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
        ShiftTime shiftTime = new ShiftTime(parseTime(request.startTime()), parseTime(request.endTime()));
        return new ShiftSchedule(userId, workDate, shiftType, shiftTime);
    }

    private LocalTime parseTime(String time) {
        return time == null ? null : LocalTime.parse(time);
    }
}
