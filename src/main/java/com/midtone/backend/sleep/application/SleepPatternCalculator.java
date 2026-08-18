package com.midtone.backend.sleep.application;

import com.midtone.backend.sleep.domain.SleepLog;
import com.midtone.backend.sleep.domain.SleepLogRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SleepPatternCalculator {

    private static final int WINDOW_DAYS = 14;
    private static final int CBT_MIN_BEFORE_WAKE_HOURS = 2;
    private static final double SECONDS_PER_DAY = 24.0 * 60.0 * 60.0;

    private final SleepLogRepository sleepLogRepository;

    public SleepPatternCalculator(SleepLogRepository sleepLogRepository) {
        this.sleepLogRepository = sleepLogRepository;
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

        LocalTime habitualBedtime = circularMean(logs.stream()
                .map(SleepLog::getSleptAt)
                .map(LocalDateTime::toLocalTime)
                .toList());
        LocalTime habitualWakeTime = circularMean(logs.stream()
                .map(SleepLog::getWokeAt)
                .map(LocalDateTime::toLocalTime)
                .toList());
        LocalTime habitualMidSleep = circularMean(logs.stream()
                .map(this::midSleep)
                .map(LocalDateTime::toLocalTime)
                .toList());

        return new SleepPattern(
                habitualBedtime,
                habitualWakeTime,
                habitualMidSleep,
                habitualWakeTime.minusHours(CBT_MIN_BEFORE_WAKE_HOURS),
                logs.size(),
                windowFrom,
                today);
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
