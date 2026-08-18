package com.midtone.backend.nutrition.application;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.nutrition.domain.NutritionTimingTag;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class NutritionTimingResolver {
    private final ShiftScheduleRepository shiftRepository; private final UserRepository userRepository; private final Clock clock;
    public NutritionTimingResolver(ShiftScheduleRepository shiftRepository, UserRepository userRepository, Clock clock) {
        this.shiftRepository = shiftRepository; this.userRepository = userRepository; this.clock = clock;
    }
    public NutritionTimingTag resolve(long userId) {
        ZoneId zone = userRepository.findById(userId).map(user -> ZoneId.of(user.getTimezone()))
                .orElse(DateTimeDefaults.DEFAULT_ZONE);
        LocalDateTime now = LocalDateTime.now(clock.withZone(zone));
        ShiftSchedule today = shiftRepository.findByUserIdAndWorkDate(userId, now.toLocalDate()).orElse(null);
        if (today != null && today.getShiftType() == ShiftType.NIGHT && today.getStartTime() != null && today.getEndTime() != null) {
            LocalDateTime start = now.toLocalDate().atTime(today.getStartTime());
            LocalDateTime end = now.toLocalDate().atTime(today.getEndTime());
            if (!end.isAfter(start)) end = end.plusDays(1);
            if (now.isBefore(start)) return NutritionTimingTag.BEFORE_NIGHT;
            if (now.isBefore(end)) return NutritionTimingTag.DURING_NIGHT;
            return NutritionTimingTag.AFTER_NIGHT;
        }
        ShiftSchedule yesterday = shiftRepository.findByUserIdAndWorkDate(userId, now.toLocalDate().minusDays(1)).orElse(null);
        if (yesterday != null && yesterday.getShiftType() == ShiftType.NIGHT
                && yesterday.getStartTime() != null && yesterday.getEndTime() != null) {
            LocalDateTime start = now.toLocalDate().minusDays(1).atTime(yesterday.getStartTime());
            LocalDateTime end = now.toLocalDate().minusDays(1).atTime(yesterday.getEndTime());
            if (!end.isAfter(start)) end = end.plusDays(1);
            if (!now.isBefore(start) && now.isBefore(end)) return NutritionTimingTag.DURING_NIGHT;
            if (!now.isBefore(end)) return NutritionTimingTag.AFTER_NIGHT;
        }
        return NutritionTimingTag.GENERAL;
    }
}
