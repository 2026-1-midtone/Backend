package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TransitionDetector {

    private static final int LOOKBACK_DAYS = 14;

    private final ShiftScheduleRepository shiftScheduleRepository;

    public TransitionDetector(ShiftScheduleRepository shiftScheduleRepository) {
        this.shiftScheduleRepository = shiftScheduleRepository;
    }

    public Optional<TransitionInfo> detectTransition(long userId, LocalDate date, ShiftType todayShiftType) {
        if (todayShiftType == ShiftType.OFF) {
            return Optional.empty();
        }
        List<ShiftSchedule> priorShifts = shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                userId, date.minusDays(LOOKBACK_DAYS), date.minusDays(1));
        return findLastNonOffType(priorShifts)
                .filter(fromType -> fromType != todayShiftType)
                .map(fromType -> new TransitionInfo(fromType, todayShiftType));
    }

    private Optional<ShiftType> findLastNonOffType(List<ShiftSchedule> shifts) {
        for (int i = shifts.size() - 1; i >= 0; i--) {
            if (shifts.get(i).getShiftType() != ShiftType.OFF) {
                return Optional.of(shifts.get(i).getShiftType());
            }
        }
        return Optional.empty();
    }

    public record TransitionInfo(ShiftType fromShiftType, ShiftType toShiftType) {
    }
}
