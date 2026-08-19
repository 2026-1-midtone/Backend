package com.midtone.backend.coaching.application;

import com.midtone.backend.caffeine.application.CaffeineStatusCalculator;
import com.midtone.backend.caffeine.application.DailyCaffeineStatus;
import com.midtone.backend.coaching.domain.CoachingCard.CoachingCardContent;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftScheduleWindow;
import com.midtone.backend.sleep.application.SleepPattern;
import com.midtone.backend.sleep.application.SleepPatternCalculator;
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

    private static final CaffeineSensitivity DEFAULT_SENSITIVITY = CaffeineSensitivity.MEDIUM;

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final TransitionDetector transitionDetector;
    private final CoachingCardGenerator coachingCardGenerator;
    private final SleepPatternCalculator sleepPatternCalculator;
    private final CaffeineStatusCalculator caffeineStatusCalculator;

    public DailyCoachingGenerator(
            ShiftScheduleRepository shiftScheduleRepository,
            UserSettingsRepository userSettingsRepository,
            TransitionDetector transitionDetector,
            CoachingCardGenerator coachingCardGenerator,
            SleepPatternCalculator sleepPatternCalculator,
            CaffeineStatusCalculator caffeineStatusCalculator) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.transitionDetector = transitionDetector;
        this.coachingCardGenerator = coachingCardGenerator;
        this.sleepPatternCalculator = sleepPatternCalculator;
        this.caffeineStatusCalculator = caffeineStatusCalculator;
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
        SleepPattern sleepPattern = sleepPatternCalculator.calculate(userId, date);
        DailyCaffeineStatus caffeineStatus = caffeineStatusCalculator.calculate(userId, date);
        List<CoachingCardContent> cards = coachingCardGenerator.generate(
                todayShift, sensitivity, preferredNapMinutes, sleepPattern, caffeineStatus);
        return new GeneratedCoaching(todayShift, nextShiftStartAt, transitionDay, sensitivity, cards);
    }

    private LocalDateTime findNextShiftStartAt(long userId, LocalDate date) {
        LocalDate searchTo = date.plusDays(ShiftScheduleWindow.SCAN_DAYS);
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
