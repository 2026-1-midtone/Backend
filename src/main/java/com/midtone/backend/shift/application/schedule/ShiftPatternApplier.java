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

    public ShiftPatternApplier(
            ShiftScheduleRepository shiftScheduleRepository,
            CurrentUserIdProvider currentUserIdProvider,
            ShiftPatternService shiftPatternService,
            ShiftCompletenessCalculator shiftCompletenessCalculator) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.currentUserIdProvider = currentUserIdProvider;
        this.shiftPatternService = shiftPatternService;
        this.shiftCompletenessCalculator = shiftCompletenessCalculator;
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
        return ApplyShiftPatternResponse.of(result.createdCount(), result.updatedCount(), patternId, completeness);
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
