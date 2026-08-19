package com.midtone.backend.routine.application;

import com.midtone.backend.coaching.domain.CoachingCard;
import com.midtone.backend.routine.domain.RoutineTask;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.transition.application.TransitionGuideResponse;
import com.midtone.backend.transition.application.TransitionPhaseType;
import com.midtone.backend.transition.application.TransitionProtocol;
import com.midtone.backend.transition.application.TransitionProtocolCatalog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RoutineTaskGenerator {

    public static final String SOURCE_TYPE_COACHING = "COACHING";
    public static final String SOURCE_TYPE_TRANSITION = "TRANSITION";

    private static final List<String> GENERATED_SOURCE_TYPES = List.of(SOURCE_TYPE_COACHING, SOURCE_TYPE_TRANSITION);

    private final RoutineTaskRepository routineTaskRepository;
    private final TransitionDetector transitionDetector;
    private final TransitionProtocolCatalog transitionProtocolCatalog;

    public RoutineTaskGenerator(
            RoutineTaskRepository routineTaskRepository,
            TransitionDetector transitionDetector,
            TransitionProtocolCatalog transitionProtocolCatalog) {
        this.routineTaskRepository = routineTaskRepository;
        this.transitionDetector = transitionDetector;
        this.transitionProtocolCatalog = transitionProtocolCatalog;
    }

    public void regenerate(
            long userId, LocalDate date, ShiftType todayShiftType, LocalTime todayShiftStartTime,
            List<CoachingCard> coachingCards) {
        routineTaskRepository.deleteAllByUserIdAndTaskDateAndSourceTypeIn(userId, date, GENERATED_SOURCE_TYPES);
        List<RoutineTask> tasks = new ArrayList<>();
        coachingCards.forEach(card -> tasks.add(toCoachingTask(userId, date, card)));
        tasks.addAll(transitionTasks(userId, date, todayShiftType, todayShiftStartTime));
        if (!tasks.isEmpty()) {
            routineTaskRepository.saveAll(tasks);
        }
    }

    private RoutineTask toCoachingTask(long userId, LocalDate date, CoachingCard card) {
        return new RoutineTask(
                userId,
                date,
                SOURCE_TYPE_COACHING,
                card.getId(),
                card.getCardType().name(),
                card.getTitle(),
                card.getDescription(),
                card.getWindowStart(),
                card.getWindowEnd());
    }

    private List<RoutineTask> transitionTasks(
            long userId, LocalDate date, ShiftType todayShiftType, LocalTime todayShiftStartTime) {
        Optional<TransitionDetector.TransitionInfo> transition =
                transitionDetector.detectTransition(userId, date, todayShiftType);
        if (transition.isEmpty()) {
            return List.of();
        }
        TransitionProtocol protocol =
                transitionProtocolCatalog.resolve(transition.get().fromShiftType(), transition.get().toShiftType());
        LocalDateTime dDayShiftStart = TransitionGuideResponse.resolveDDayShiftStart(
                date, transition.get().toShiftType(), todayShiftStartTime);
        return protocol.phases().stream()
                .filter(phase -> phase.phase() == TransitionPhaseType.D_DAY)
                .flatMap(phase -> phase.steps().stream())
                .map(step -> toTransitionTask(userId, date, protocol.protocolName(), step, dDayShiftStart))
                .toList();
    }

    private RoutineTask toTransitionTask(
            long userId, LocalDate date, String protocolName, TransitionProtocol.StepTemplate step,
            LocalDateTime dDayShiftStart) {
        LocalDateTime windowStart = dDayShiftStart.plus(step.windowStartOffset());
        LocalDateTime windowEnd = dDayShiftStart.plus(step.windowEndOffset());
        return new RoutineTask(
                userId,
                date,
                SOURCE_TYPE_TRANSITION,
                null,
                step.category().name(),
                step.actionText(),
                protocolName,
                windowStart,
                windowEnd);
    }
}
