package com.midtone.backend.transition.application;

import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record TransitionGuideResponse(
        String transitionDate,
        String fromShiftType,
        String toShiftType,
        String protocolName,
        String description,
        List<Phase> phases,
        String linkedRoutineDate,
        String disclaimer) {

    private static final String DISCLAIMER = "의학적 치료·약물 권고를 포함하지 않습니다.";

    // 실제 근무시작시각을 모를 때(시각 미기록)만 쓰는 대표값. 정상적으로는 사용자의 실제 근무표 시각을 그대로 씀.
    private static final Map<ShiftType, LocalTime> DEFAULT_SHIFT_START = Map.of(
            ShiftType.DAY, LocalTime.of(9, 0),
            ShiftType.EVENING, LocalTime.of(12, 0),
            ShiftType.NIGHT, LocalTime.of(22, 0));

    /**
     * D-Day(전환일) 새 근무 시작시각을 계산한다. 실제 근무시각을 모를 때만 대표값으로 대체한다.
     * {@link com.midtone.backend.routine.application.RoutineTaskGenerator}에서도 동일 기준으로 재사용한다.
     */
    public static LocalDateTime resolveDDayShiftStart(LocalDate date, ShiftType toShiftType, LocalTime actualShiftStartTime) {
        LocalTime startTime = actualShiftStartTime != null
                ? actualShiftStartTime
                : DEFAULT_SHIFT_START.get(toShiftType);
        return date.atTime(startTime);
    }

    public static TransitionGuideResponse of(
            LocalDate date, TransitionDetector.TransitionInfo info, TransitionProtocol protocol,
            LocalTime actualShiftStartTime) {
        LocalDateTime dDayShiftStart = resolveDDayShiftStart(date, info.toShiftType(), actualShiftStartTime);
        List<Phase> phases = protocol.phases().stream().map(phase -> Phase.from(phase, dDayShiftStart)).toList();
        return new TransitionGuideResponse(
                date.toString(),
                info.fromShiftType().name(),
                info.toShiftType().name(),
                protocol.protocolName(),
                protocol.description(),
                phases,
                date.toString(),
                DISCLAIMER);
    }

    public record Phase(String phase, String label, List<Step> steps) {

        public static Phase from(TransitionProtocol.PhaseTemplate template, LocalDateTime dDayShiftStart) {
            List<Step> steps = template.steps().stream().map(step -> Step.from(step, dDayShiftStart)).toList();
            return new Phase(template.phase().name(), template.label(), steps);
        }
    }

    public record Step(int stepId, String category, String windowStart, String windowEnd, String actionText) {

        public static Step from(TransitionProtocol.StepTemplate template, LocalDateTime dDayShiftStart) {
            return new Step(
                    template.stepId(),
                    template.category().name(),
                    dDayShiftStart.plus(template.windowStartOffset()).toString(),
                    dDayShiftStart.plus(template.windowEndOffset()).toString(),
                    template.actionText());
        }
    }
}
