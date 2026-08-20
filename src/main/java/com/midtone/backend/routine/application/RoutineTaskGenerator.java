package com.midtone.backend.routine.application;

import com.midtone.backend.coaching.domain.CoachingCard;
import com.midtone.backend.routine.domain.RoutineTask;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
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
    public static final String SOURCE_TYPE_BASELINE = "BASELINE";

    private static final List<String> GENERATED_SOURCE_TYPES =
            List.of(SOURCE_TYPE_COACHING, SOURCE_TYPE_TRANSITION, SOURCE_TYPE_BASELINE);

    private final RoutineTaskRepository routineTaskRepository;
    private final TransitionDetector transitionDetector;
    private final TransitionProtocolCatalog transitionProtocolCatalog;
    private final BaselineRoutineCatalog baselineRoutineCatalog;

    public RoutineTaskGenerator(
            RoutineTaskRepository routineTaskRepository,
            TransitionDetector transitionDetector,
            TransitionProtocolCatalog transitionProtocolCatalog,
            BaselineRoutineCatalog baselineRoutineCatalog) {
        this.routineTaskRepository = routineTaskRepository;
        this.transitionDetector = transitionDetector;
        this.transitionProtocolCatalog = transitionProtocolCatalog;
        this.baselineRoutineCatalog = baselineRoutineCatalog;
    }

    public void regenerate(long userId, LocalDate date, ShiftSchedule todayShift, List<CoachingCard> coachingCards) {
        routineTaskRepository.deleteAllByUserIdAndTaskDateAndSourceTypeIn(userId, date, GENERATED_SOURCE_TYPES);
        List<RoutineTask> tasks = new ArrayList<>();
        coachingCards.forEach(card -> tasks.add(toCoachingTask(userId, date, card)));
        tasks.addAll(baselineTasks(userId, date, todayShift));
        tasks.addAll(transitionTasks(userId, date, todayShift.getShiftType(), todayShift.getStartTime()));
        if (!tasks.isEmpty()) {
            routineTaskRepository.saveAll(tasks);
        }
    }

    /**
     * 근무 시각을 기준으로 잡는 매일의 기본 루틴. 근무 시각이 비어 있으면 앵커가 없어 만들 수 없으므로,
     * 오프와 마찬가지로 절대 시각 템플릿을 쓴다.
     */
    private List<RoutineTask> baselineTasks(long userId, LocalDate date, ShiftSchedule todayShift) {
        boolean hasShiftTimes = todayShift.getStartTime() != null && todayShift.getEndTime() != null;
        if (todayShift.getShiftType() == ShiftType.OFF || !hasShiftTimes) {
            return baselineRoutineCatalog.resolveOff().stream()
                    .map(step -> new RoutineTask(
                            userId, date, SOURCE_TYPE_BASELINE, null, step.category(), step.title(), null,
                            date.atTime(step.windowStart()), date.atTime(step.windowEnd())))
                    .toList();
        }
        LocalDateTime shiftStart = date.atTime(todayShift.getStartTime());
        LocalDateTime shiftEnd = date.atTime(todayShift.getEndTime());
        LocalDateTime resolvedEnd = shiftEnd.isBefore(shiftStart) ? shiftEnd.plusDays(1) : shiftEnd;
        return baselineRoutineCatalog.resolve(todayShift.getShiftType()).stream()
                .map(step -> {
                    LocalDateTime anchor =
                            step.anchor() == BaselineRoutineCatalog.Step.Anchor.SHIFT_START ? shiftStart : resolvedEnd;
                    return new RoutineTask(
                            userId, date, SOURCE_TYPE_BASELINE, null, step.category(), step.title(), null,
                            anchor.plus(step.startOffset()), anchor.plus(step.endOffset()));
                })
                .toList();
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
