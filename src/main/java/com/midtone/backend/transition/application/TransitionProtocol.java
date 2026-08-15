package com.midtone.backend.transition.application;

import java.util.List;

public record TransitionProtocol(String protocolName, String description, List<PhaseTemplate> phases) {

    public record PhaseTemplate(TransitionPhaseType phase, String label, List<StepTemplate> steps) {
    }

    public record StepTemplate(
            int stepId, TransitionStepCategory category, String windowStart, String windowEnd, String actionText) {
    }
}
