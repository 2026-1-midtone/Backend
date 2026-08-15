package com.midtone.backend.coaching.application;

import com.midtone.backend.coaching.domain.CoachingCard;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.user.domain.CaffeineSensitivity;
import java.time.LocalDateTime;
import java.util.List;

public record GeneratedCoaching(
        ShiftSchedule todayShift,
        LocalDateTime nextShiftStartAt,
        boolean transitionDay,
        CaffeineSensitivity caffeineSensitivity,
        List<CoachingCard.CoachingCardContent> cards) {
}
