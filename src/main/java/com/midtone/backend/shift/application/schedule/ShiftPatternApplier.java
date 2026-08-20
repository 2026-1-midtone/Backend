package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.application.pattern.SaveShiftPatternRequest;
import com.midtone.backend.shift.application.pattern.ShiftPatternResponse;
import com.midtone.backend.shift.application.pattern.ShiftPatternService;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftScheduleWindow;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ShiftPatternApplier {

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final CurrentUserIdProvider currentUserIdProvider;
    private final ShiftPatternService shiftPatternService;
    private final ShiftCompletenessCalculator shiftCompletenessCalculator;
    private final ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger;
    private final ShiftTimeDefaultService shiftTimeDefaultService;

    public ShiftPatternApplier(
            ShiftScheduleRepository shiftScheduleRepository,
            CurrentUserIdProvider currentUserIdProvider,
            ShiftPatternService shiftPatternService,
            ShiftCompletenessCalculator shiftCompletenessCalculator,
            ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger,
            ShiftTimeDefaultService shiftTimeDefaultService) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.currentUserIdProvider = currentUserIdProvider;
        this.shiftPatternService = shiftPatternService;
        this.shiftCompletenessCalculator = shiftCompletenessCalculator;
        this.shiftCoachingRegenerationTrigger = shiftCoachingRegenerationTrigger;
        this.shiftTimeDefaultService = shiftTimeDefaultService;
    }

    @Transactional
    public ApplyShiftPatternResponse apply(ApplyShiftPatternRequest request) {
        validatePatternName(request.saveAsPattern(), request.patternName());
        long userId = currentUserIdProvider.getCurrentUserId();
        LocalDate startDate = LocalDate.parse(request.startDate());
        LocalDate endDate = startDate.plusDays((long) request.weeks() * ShiftScheduleWindow.DAYS_PER_WEEK - 1);
        List<ShiftType> patternTypes = toShiftTypes(request.pattern());
        ApplyResult result = applyPatternToRange(userId, startDate, endDate, patternTypes);
        Long patternId = saveAsPatternIfRequested(request);
        CompletenessResponse completeness = shiftCompletenessCalculator.calculate(startDate, endDate);
        List<String> affectedCoachingDates = shiftCoachingRegenerationTrigger.triggerForRange(startDate, endDate);
        return ApplyShiftPatternResponse.of(
                result.createdCount(), result.updatedCount(), patternId, completeness, affectedCoachingDates);
    }

    private void validatePatternName(Boolean saveAsPattern, String patternName) {
        boolean missingName = Boolean.TRUE.equals(saveAsPattern) && (patternName == null || patternName.isBlank());
        if (missingName) {
            throw new ShiftException(ShiftException.ErrorCode.PATTERN_NAME_REQUIRED);
        }
    }

    private List<ShiftType> toShiftTypes(List<String> pattern) {
        return pattern.stream().map(ShiftType::valueOf).toList();
    }

    private ApplyResult applyPatternToRange(
            long userId, LocalDate startDate, LocalDate endDate, List<ShiftType> patternTypes) {
        Map<LocalDate, ShiftType> desiredTypes = buildDesiredTypes(startDate, endDate, patternTypes);
        Map<ShiftType, ShiftTime> defaultTimes = resolveDefaultTimes(userId, patternTypes);
        Map<LocalDate, ShiftSchedule> existingShifts = findExistingShifts(userId, startDate, endDate);
        int updatedCount = updateExistingShifts(existingShifts, desiredTypes, defaultTimes);
        List<ShiftSchedule> newShifts = buildNewShifts(userId, desiredTypes, existingShifts, defaultTimes);
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

    /**
     * 유형이 바뀐 날만 시각까지 새 유형의 기본값으로 덮어쓴다.
     * 유형이 그대로인 날은 사용자가 그 날만 따로 손봤을 수 있어 시각을 건드리지 않는다.
     */
    /** 유형은 최대 4가지뿐이라 날짜마다 조회하지 않고 유형별로 한 번만 읽는다. */
    private Map<ShiftType, ShiftTime> resolveDefaultTimes(long userId, List<ShiftType> patternTypes) {
        Map<ShiftType, ShiftTime> defaultTimes = new EnumMap<>(ShiftType.class);
        patternTypes.forEach(
                shiftType -> defaultTimes.computeIfAbsent(
                        shiftType, type -> shiftTimeDefaultService.resolve(userId, type)));
        return defaultTimes;
    }

    private int updateExistingShifts(
            Map<LocalDate, ShiftSchedule> existingShifts,
            Map<LocalDate, ShiftType> desiredTypes,
            Map<ShiftType, ShiftTime> defaultTimes) {
        int updatedCount = 0;
        for (Map.Entry<LocalDate, ShiftSchedule> entry : existingShifts.entrySet()) {
            ShiftSchedule shift = entry.getValue();
            ShiftType desiredType = desiredTypes.get(entry.getKey());
            if (shift.getShiftType() != desiredType) {
                shift.changeType(desiredType, defaultTimes.get(desiredType));
            }
            updatedCount++;
        }
        return updatedCount;
    }

    private List<ShiftSchedule> buildNewShifts(
            long userId,
            Map<LocalDate, ShiftType> desiredTypes,
            Map<LocalDate, ShiftSchedule> existingShifts,
            Map<ShiftType, ShiftTime> defaultTimes) {
        return desiredTypes.entrySet().stream()
                .filter(entry -> !existingShifts.containsKey(entry.getKey()))
                .map(entry -> new ShiftSchedule(
                        userId, entry.getKey(), entry.getValue(), defaultTimes.get(entry.getValue())))
                .toList();
    }

    private Long saveAsPatternIfRequested(ApplyShiftPatternRequest request) {
        if (!Boolean.TRUE.equals(request.saveAsPattern())) {
            return null;
        }
        ShiftPatternResponse response = shiftPatternService.saveShiftPattern(
                new SaveShiftPatternRequest(request.patternName(), request.pattern()));
        return response.patternId();
    }

    private record ApplyResult(int createdCount, int updatedCount) {}
}
