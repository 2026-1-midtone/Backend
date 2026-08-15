package com.midtone.backend.coaching.application;

import com.midtone.backend.coaching.domain.CoachingCard.CoachingCardContent;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.user.domain.CaffeineSensitivity;
import com.midtone.backend.user.domain.UserSettings;
import com.midtone.backend.user.domain.UserSettingsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DailyCoachingGenerator {

    private static final int NEXT_SHIFT_LOOKAHEAD_DAYS = 14;
    private static final CaffeineSensitivity DEFAULT_SENSITIVITY = CaffeineSensitivity.MEDIUM;

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final TransitionDetector transitionDetector;
    private final CoachingCardGenerator coachingCardGenerator;

    public DailyCoachingGenerator(
            ShiftScheduleRepository shiftScheduleRepository,
            UserSettingsRepository userSettingsRepository,
            TransitionDetector transitionDetector,
            CoachingCardGenerator coachingCardGenerator) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.transitionDetector = transitionDetector;
        this.coachingCardGenerator = coachingCardGenerator;
    }

    public Optional<GeneratedCoaching> generate(long userId, LocalDate date) {
        return shiftScheduleRepository.findByUserIdAndWorkDate(userId, date)
                .map(todayShift -> buildGeneratedCoaching(userId, date, todayShift));
    }

    private GeneratedCoaching buildGeneratedCoaching(long userId, LocalDate date, ShiftSchedule todayShift) {
        LocalDateTime nextShiftStartAt = findNextShiftStartAt(userId, date);
        UserSettings settings = userSettingsRepository.findById(userId).orElse(null);
        CaffeineSensitivity sensitivity = sensitivityOf(settings);
        int preferredNapMinutes = napMinutesOf(settings);
        boolean transitionDay =
                transitionDetector.detectTransition(userId, date, todayShift.getShiftType()).isPresent();
        List<CoachingCardContent> cards = coachingCardGenerator.generate(todayShift, sensitivity, preferredNapMinutes);
        return new GeneratedCoaching(todayShift, nextShiftStartAt, transitionDay, sensitivity, cards);
    }

    private LocalDateTime findNextShiftStartAt(long userId, LocalDate date) {
        LocalDate searchTo = date.plusDays(NEXT_SHIFT_LOOKAHEAD_DAYS);
        List<ShiftSchedule> upcoming = shiftScheduleRepository
                .findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(userId, date.plusDays(1), searchTo);
        return upcoming.stream()
                .filter(shift -> shift.getStartTime() != null)
                .findFirst()
                .map(shift -> shift.getWorkDate().atTime(shift.getStartTime()))
                .orElse(null);
    }

    private CaffeineSensitivity sensitivityOf(UserSettings settings) {
        if (settings == null || settings.getCaffeineSensitivity() == null) {
            return DEFAULT_SENSITIVITY;
        }
        return settings.getCaffeineSensitivity();
    }

    private int napMinutesOf(UserSettings settings) {
        return settings == null ? UserSettings.DEFAULT_PREFERRED_NAP_MINUTES : settings.getPreferredNapMinutes();
    }
}
