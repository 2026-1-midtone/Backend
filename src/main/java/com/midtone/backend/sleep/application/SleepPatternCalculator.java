package com.midtone.backend.sleep.application;

import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.sleep.domain.SleepLog;
import com.midtone.backend.sleep.domain.SleepLogRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SleepPatternCalculator {

    private static final int WINDOW_DAYS = 14;
    private static final int CBT_MIN_BEFORE_WAKE_HOURS = 2;
    private static final double SECONDS_PER_DAY = 24.0 * 60.0 * 60.0;

    /**
     * 오늘과 같은 근무 유형(예: 나이트 근무) 뒤에 잔 기록만 걸러서 평균 낼 때 필요한 최소 표본 수.
     * 임상 근거로 도출한 값이 아니라, 표본 1~2개로 평균을 내면 하루 컨디션에 따라 값이 크게 흔들려
     * 신뢰하기 어렵다는 실무적 판단으로 정한 최소치다.
     */
    private static final int MIN_SAMPLES_FOR_SHIFT_MATCH = 3;

    /**
     * 근무가 끝난 뒤 이 시간 안에 잠들었으면 "그 근무 때문에 생긴 수면"으로 본다. 통근·씻기·긴장 이완 등
     * 준비 시간을 감안한 여유값이며, 특정 연구를 근거로 한 값은 아니다.
     */
    private static final Duration POST_SHIFT_SLEEP_WINDOW = Duration.ofHours(6);

    private final SleepLogRepository sleepLogRepository;
    private final ShiftScheduleRepository shiftScheduleRepository;

    public SleepPatternCalculator(
            SleepLogRepository sleepLogRepository, ShiftScheduleRepository shiftScheduleRepository) {
        this.sleepLogRepository = sleepLogRepository;
        this.shiftScheduleRepository = shiftScheduleRepository;
    }

    public SleepPattern calculate(long userId, LocalDate today) {
        Objects.requireNonNull(today, "today must not be null");
        LocalDate windowFrom = today.minusDays(WINDOW_DAYS - 1L);
        List<SleepLog> logs = sleepLogRepository
                .findByUserIdAndWokeAtGreaterThanEqualAndWokeAtLessThanOrderByWokeAtAsc(
                        userId, windowFrom.atStartOfDay(), today.plusDays(1).atStartOfDay());

        if (logs.isEmpty()) {
            return new SleepPattern(null, null, null, null, 0, windowFrom, today);
        }

        List<SleepLog> effectiveLogs = selectLogsForToday(userId, today, windowFrom, logs);
        if (effectiveLogs.isEmpty()) {
            // 오늘과 같은 근무 유형 뒤 수면 기록이 최소 표본에 못 미친다. 서로 다른 근무 유형의 취침 시각을
            // 섞어 평균 내면 어느 쪽에도 맞지 않는 값이 나오므로, 표본이 더 쌓일 때까지는 추정하지 않는다.
            return new SleepPattern(null, null, null, null, 0, windowFrom, today);
        }

        LocalTime habitualBedtime = circularMean(effectiveLogs.stream()
                .map(SleepLog::getSleptAt)
                .map(LocalDateTime::toLocalTime)
                .toList());
        LocalTime habitualWakeTime = circularMean(effectiveLogs.stream()
                .map(SleepLog::getWokeAt)
                .map(LocalDateTime::toLocalTime)
                .toList());
        LocalTime habitualMidSleep = circularMean(effectiveLogs.stream()
                .map(this::midSleep)
                .map(LocalDateTime::toLocalTime)
                .toList());

        return new SleepPattern(
                habitualBedtime,
                habitualWakeTime,
                habitualMidSleep,
                habitualWakeTime.minusHours(CBT_MIN_BEFORE_WAKE_HOURS),
                effectiveLogs.size(),
                windowFrom,
                today);
    }

    /**
     * 오늘과 같은 근무 유형 뒤에 잔 기록만 추린다. 근무 유형별로 실제 취침 시각대가 완전히 다르므로
     * (예: 나이트 근무 뒤엔 낮에, 데이 근무 뒤엔 밤에) 서로 다른 유형을 섞어 평균 내면 어느 쪽에도 맞지 않는
     * 값이 나온다. 오늘 근무 일정을 모르면(미등록) 걸러낼 기준이 없어 어쩔 수 없이 전체 기록으로 근사한다.
     */
    private List<SleepLog> selectLogsForToday(long userId, LocalDate today, LocalDate windowFrom, List<SleepLog> logs) {
        ShiftType todayType = shiftScheduleRepository.findByUserIdAndWorkDate(userId, today)
                .map(ShiftSchedule::getShiftType)
                .orElse(null);
        if (todayType == null) {
            return logs;
        }

        Map<LocalDate, ShiftSchedule> schedulesByDate = shiftScheduleRepository
                .findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(userId, windowFrom.minusDays(1), today)
                .stream()
                .collect(Collectors.toMap(ShiftSchedule::getWorkDate, schedule -> schedule, (a, b) -> a));

        List<SleepLog> matched = logs.stream()
                .filter(log -> todayType == classifySleepShiftType(log.getSleptAt(), schedulesByDate))
                .toList();
        return matched.size() >= MIN_SAMPLES_FOR_SHIFT_MATCH ? matched : List.of();
    }

    /**
     * 이 수면이 어느 근무 유형 뒤에 이어진 잠인지 추정한다. 수면 기록에 근무를 직접 연결하는 FK가 없어서,
     * 전날/당일 일정에서 역산한다: 전날 시작한 근무가 자정을 넘겨 끝난 직후 잠들었으면 그 근무 유형(나이트
     * 근무 케이스), 아니면 당일 근무가 끝난 직후 잠들었으면 그 근무 유형(데이·이브닝 근무 케이스),
     * 당일이 OFF면 OFF로 본다.
     */
    private ShiftType classifySleepShiftType(LocalDateTime sleptAt, Map<LocalDate, ShiftSchedule> schedulesByDate) {
        LocalDate sleptDate = sleptAt.toLocalDate();
        ShiftType fromPreviousDay =
                classifyIfShiftEndedJustBefore(schedulesByDate.get(sleptDate.minusDays(1)), sleptAt);
        if (fromPreviousDay != null) {
            return fromPreviousDay;
        }
        ShiftSchedule sameDay = schedulesByDate.get(sleptDate);
        if (sameDay != null && sameDay.getShiftType() == ShiftType.OFF) {
            return ShiftType.OFF;
        }
        return classifyIfShiftEndedJustBefore(sameDay, sleptAt);
    }

    private ShiftType classifyIfShiftEndedJustBefore(ShiftSchedule shift, LocalDateTime sleptAt) {
        if (shift == null || shift.getStartTime() == null || shift.getEndTime() == null) {
            return null;
        }
        LocalDateTime shiftEnd = shiftEndDateTime(shift);
        if (shiftEnd.isAfter(sleptAt)) {
            return null;
        }
        Duration gap = Duration.between(shiftEnd, sleptAt);
        return gap.compareTo(POST_SHIFT_SLEEP_WINDOW) <= 0 ? shift.getShiftType() : null;
    }

    private LocalDateTime shiftEndDateTime(ShiftSchedule shift) {
        LocalDateTime start = shift.getWorkDate().atTime(shift.getStartTime());
        LocalDateTime end = shift.getWorkDate().atTime(shift.getEndTime());
        return !end.isAfter(start) ? end.plusDays(1) : end;
    }

    private LocalDateTime midSleep(SleepLog log) {
        Duration sleepDuration = Duration.between(log.getSleptAt(), log.getWokeAt());
        return log.getSleptAt().plusNanos(sleepDuration.toNanos() / 2);
    }

    private LocalTime circularMean(List<LocalTime> times) {
        double sinSum = 0.0;
        double cosSum = 0.0;
        for (LocalTime time : times) {
            double angle = time.toNanoOfDay() / (SECONDS_PER_DAY * 1_000_000_000.0) * 2.0 * Math.PI;
            sinSum += Math.sin(angle);
            cosSum += Math.cos(angle);
        }

        double meanAngle = Math.atan2(sinSum / times.size(), cosSum / times.size());
        if (meanAngle < 0) {
            meanAngle += 2.0 * Math.PI;
        }
        long secondOfDay = Math.round(meanAngle / (2.0 * Math.PI) * SECONDS_PER_DAY) % (long) SECONDS_PER_DAY;
        return LocalTime.ofSecondOfDay(secondOfDay);
    }
}
