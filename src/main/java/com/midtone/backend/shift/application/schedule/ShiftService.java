package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.domain.ShiftPattern;
import com.midtone.backend.shift.domain.ShiftPatternRepository;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private static final int MAX_BULK_RANGE_DAYS = 90;
    private static final int DAYS_PER_WEEK = 7;

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final ShiftPatternRepository shiftPatternRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public ShiftService(
            ShiftScheduleRepository shiftScheduleRepository,
            ShiftPatternRepository shiftPatternRepository,
            CurrentUserIdProvider currentUserIdProvider) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.shiftPatternRepository = shiftPatternRepository;
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

    @Transactional
    public ApplyShiftPatternResponse applyShiftPattern(ApplyShiftPatternRequest request) {
        validatePatternName(request.saveAsPattern(), request.patternName());
        long userId = currentUserIdProvider.getCurrentUserId();
        LocalDate startDate = LocalDate.parse(request.startDate());
        LocalDate endDate = startDate.plusDays((long) request.weeks() * DAYS_PER_WEEK - 1);
        List<ShiftType> patternTypes = toShiftTypes(request.pattern());
        ApplyResult result = applyPatternToRange(userId, startDate, endDate, patternTypes);
        Long patternId = saveAsPatternIfRequested(request, userId, patternTypes);
        CompletenessResponse completeness = calculateCompleteness(userId, startDate, endDate);
        return ApplyShiftPatternResponse.of(result.createdCount(), result.updatedCount(), patternId, completeness);
    }

    @Transactional(readOnly = true)
    public CompletenessResponse getCompleteness(int weeks) {
        long userId = currentUserIdProvider.getCurrentUserId();
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays((long) weeks * DAYS_PER_WEEK - 1);
        return calculateCompleteness(userId, from, to);
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

    private void validatePatternName(Boolean saveAsPattern, String patternName) {
        boolean missingName = Boolean.TRUE.equals(saveAsPattern) && (patternName == null || patternName.isBlank());
        if (missingName) {
            throw new ShiftException(ShiftException.ErrorCode.PATTERN_NAME_REQUIRED);
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

    private List<ShiftType> toShiftTypes(List<String> pattern) {
        return pattern.stream().map(ShiftType::valueOf).toList();
    }

    private ApplyResult applyPatternToRange(
            long userId, LocalDate startDate, LocalDate endDate, List<ShiftType> patternTypes) {
        Map<LocalDate, ShiftType> desiredTypes = buildDesiredTypes(startDate, endDate, patternTypes);
        Map<LocalDate, ShiftSchedule> existingShifts = findExistingShifts(userId, startDate, endDate);
        int updatedCount = updateExistingShifts(existingShifts, desiredTypes);
        List<ShiftSchedule> newShifts = buildNewShifts(userId, desiredTypes, existingShifts);
        shiftScheduleRepository.saveAll(newShifts);
        return new ApplyResult(newShifts.size(), updatedCount);
    }

    private Map<LocalDate, ShiftType> buildDesiredTypes(
            LocalDate startDate, LocalDate endDate, List<ShiftType> patternTypes) {
        Map<LocalDate, ShiftType> desiredTypes = new LinkedHashMap<>();
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        for (long dayIndex = 0; dayIndex < totalDays; dayIndex++) {
            LocalDate date = startDate.plusDays(dayIndex);
            desiredTypes.put(date, patternTypes.get((int) (dayIndex % patternTypes.size())));
        }
        return desiredTypes;
    }

    private Map<LocalDate, ShiftSchedule> findExistingShifts(long userId, LocalDate startDate, LocalDate endDate) {
        List<ShiftSchedule> shifts =
                shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(userId, startDate, endDate);
        return shifts.stream().collect(Collectors.toMap(ShiftSchedule::getWorkDate, Function.identity()));
    }

    private int updateExistingShifts(
            Map<LocalDate, ShiftSchedule> existingShifts, Map<LocalDate, ShiftType> desiredTypes) {
        int updatedCount = 0;
        for (Map.Entry<LocalDate, ShiftSchedule> entry : existingShifts.entrySet()) {
            entry.getValue().update(desiredTypes.get(entry.getKey()), null, null);
            updatedCount++;
        }
        return updatedCount;
    }

    private List<ShiftSchedule> buildNewShifts(
            long userId, Map<LocalDate, ShiftType> desiredTypes, Map<LocalDate, ShiftSchedule> existingShifts) {
        return desiredTypes.entrySet().stream()
                .filter(entry -> !existingShifts.containsKey(entry.getKey()))
                .map(entry -> new ShiftSchedule(userId, entry.getKey(), entry.getValue(), new ShiftTime(null, null)))
                .toList();
    }

    private Long saveAsPatternIfRequested(
            ApplyShiftPatternRequest request, long userId, List<ShiftType> patternTypes) {
        if (!Boolean.TRUE.equals(request.saveAsPattern())) {
            return null;
        }
        ShiftPattern shiftPattern = new ShiftPattern(userId, request.patternName(), patternTypes);
        shiftPatternRepository.save(shiftPattern);
        return shiftPattern.getId();
    }

    private CompletenessResponse calculateCompleteness(long userId, LocalDate from, LocalDate to) {
        List<ShiftSchedule> shifts =
                shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(userId, from, to);
        Set<LocalDate> confirmedDates = shifts.stream()
                .filter(ShiftSchedule::isConfirmed)
                .map(ShiftSchedule::getWorkDate)
                .collect(Collectors.toSet());
        List<LocalDate> allDates = from.datesUntil(to.plusDays(1)).toList();
        List<LocalDate> missingDates = allDates.stream().filter(date -> !confirmedDates.contains(date)).toList();
        return CompletenessResponse.of(allDates.size(), confirmedDates.size(), missingDates);
    }

    private record ApplyResult(int createdCount, int updatedCount) {}
}
