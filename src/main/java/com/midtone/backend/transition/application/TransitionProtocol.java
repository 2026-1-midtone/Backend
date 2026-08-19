package com.midtone.backend.transition.application;

import java.time.Duration;
import java.util.List;

public record TransitionProtocol(String protocolName, String description, List<PhaseTemplate> phases) {

    public record PhaseTemplate(TransitionPhaseType phase, String label, List<StepTemplate> steps) {
    }

    /**
     * windowStartOffset/windowEndOffset은 "전환 당일(D-Day) 새 근무의 시작시각"을 기준(0)으로 한 상대 오프셋이다.
     * 음수는 그 이전, 양수는 그 이후를 의미하며 날짜 경계를 자유롭게 넘나들 수 있다(예: -25h는 D-1의 새벽).
     * 실제 시각은 {@link TransitionProtocolCatalog}가 아니라, 사용자의 실제 근무시작시각을 아는
     * 호출부(TransitionGuideResponse)에서 오프셋을 더해 계산한다 — 그래야 전 사용자 동일 고정값 문제가 사라진다.
     */
    public record StepTemplate(
            int stepId, TransitionStepCategory category, Duration windowStartOffset, Duration windowEndOffset,
            String actionText) {
    }
}
