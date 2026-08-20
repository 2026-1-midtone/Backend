package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftScheduleWindow;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 아직 시작하지 않은 가장 가까운 근무. 그날 근무도 시작 전이면 후보에 넣는다.
 * 저녁 출근을 앞둔 아침에 "다음 근무"가 비어 보이면 안 되기 때문이다.
 * 홈과 코칭이 같은 답을 내야 해서 한곳에 모아 둔다.
 */
@Component
public class NextShiftFinder {

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final Clock clock;

    public NextShiftFinder(ShiftScheduleRepository shiftScheduleRepository, Clock clock) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.clock = clock;
    }

    public Optional<ShiftSchedule> find(long userId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ShiftSchedule> upcoming = shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                userId, date, date.plusDays(ShiftScheduleWindow.SCAN_DAYS));
        return upcoming.stream()
                .filter(NextShiftFinder::isSchedulable)
                .filter(shift -> startAtOf(shift).isAfter(now))
                .findFirst();
    }

    public LocalDateTime findStartAt(long userId, LocalDate date) {
        return find(userId, date).map(NextShiftFinder::startAtOf).orElse(null);
    }

    public static LocalDateTime startAtOf(ShiftSchedule shift) {
        return shift.getWorkDate().atTime(shift.getStartTime());
    }

    private static boolean isSchedulable(ShiftSchedule shift) {
        return shift.getShiftType() != ShiftType.OFF && shift.getStartTime() != null;
    }
}
