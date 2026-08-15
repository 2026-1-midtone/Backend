package com.midtone.backend.transition.application;

import com.midtone.backend.shift.application.schedule.TransitionDetector;
import java.time.LocalDate;
import java.util.List;

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

    public static TransitionGuideResponse of(
            LocalDate date, TransitionDetector.TransitionInfo info, TransitionProtocol protocol) {
        List<Phase> phases = protocol.phases().stream().map(Phase::from).toList();
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

        public static Phase from(TransitionProtocol.PhaseTemplate template) {
            List<Step> steps = template.steps().stream().map(Step::from).toList();
            return new Phase(template.phase().name(), template.label(), steps);
        }
    }

    public record Step(int stepId, String category, String windowStart, String windowEnd, String actionText) {

        public static Step from(TransitionProtocol.StepTemplate template) {
            return new Step(
                    template.stepId(),
                    template.category().name(),
                    template.windowStart(),
                    template.windowEnd(),
                    template.actionText());
        }
    }
}
