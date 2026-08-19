package com.midtone.backend.transition.application;

import com.midtone.backend.shift.domain.ShiftType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 전환일 프로토콜 마스터 콘텐츠. 각 단계의 시각은 더 이상 고정값이 아니라, "D-Day(전환일) 새 근무 시작시각"을
 * 0으로 하는 상대 오프셋(Duration)으로 정의된다. 실제 사용자별 시각은 호출부(TransitionGuideResponse,
 * RoutineTaskGenerator)에서 오프셋 + 실제(또는 대표) 근무시작시각으로 계산한다.
 * 당김(advance) 프로토콜은 대표 근무시작시각 09:00(DAY)을, 늦춤(delay) 프로토콜은 22:00(NIGHT)을 기준으로
 * 오프셋을 설계했다 — 두 대표값은 {@link TransitionGuideResponse}의 DEFAULT_SHIFT_START와 일치한다.
 * 즉 "근무시작 대비 몇 시간 전/후"라는 프로토콜의 의미는 그대로이고, 계산 기준만 고정 시각 → 실제 근무시작시각으로 바뀐 것이다.
 */
@Component
public class TransitionProtocolCatalog {

    private static final Map<ShiftType, String> KOREAN_LABELS =
            Map.of(ShiftType.DAY, "데이", ShiftType.EVENING, "이브닝", ShiftType.NIGHT, "나이트");

    public TransitionProtocol resolve(ShiftType from, ShiftType to) {
        String protocolName = KOREAN_LABELS.get(from) + " → " + KOREAN_LABELS.get(to) + " 전환";
        boolean advancing = from.ordinal() > to.ordinal();
        return advancing ? advanceProtocol(protocolName) : delayProtocol(protocolName);
    }

    private TransitionProtocol advanceProtocol(String protocolName) {
        List<TransitionProtocol.PhaseTemplate> phases = List.of(advancePrevDay(), advanceDDay(), advanceNextDay());
        return new TransitionProtocol(protocolName, "전환 전후 3일에 걸쳐 수면 시각을 앞당기는 프로토콜입니다.", phases);
    }

    private TransitionProtocol.PhaseTemplate advancePrevDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(
                new TransitionProtocol.StepTemplate(
                        1, TransitionStepCategory.SLEEP, Duration.ofMinutes(-27 * 60), Duration.ofMinutes(-23 * 60),
                        "퇴근 후 4시간만 앵커 수면"),
                new TransitionProtocol.StepTemplate(
                        2, TransitionStepCategory.LIGHT, Duration.ofMinutes(-19 * 60), Duration.ofMinutes(-18 * 60 - 30),
                        "오후 야외 빛 노출 30분"),
                new TransitionProtocol.StepTemplate(
                        3, TransitionStepCategory.CAFFEINE, Duration.ofMinutes(-20 * 60 - 30), Duration.ofMinutes(-20 * 60),
                        "카페인 섭취 중단"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.PREV_DAY, "전환 전날", steps);
    }

    private TransitionProtocol.PhaseTemplate advanceDDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(
                new TransitionProtocol.StepTemplate(
                        4, TransitionStepCategory.MEAL, Duration.ofMinutes(-90), Duration.ofMinutes(-30),
                        "가벼운 아침 식사로 리듬 전환 신호주기"),
                new TransitionProtocol.StepTemplate(
                        5, TransitionStepCategory.LIGHT, Duration.ofMinutes(-150), Duration.ofMinutes(-130),
                        "기상 후 아침 빛 노출 20분"),
                new TransitionProtocol.StepTemplate(
                        6, TransitionStepCategory.SLEEP, Duration.ofHours(13), Duration.ofHours(21),
                        "목표 취침 시각에 맞춰 취침"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.D_DAY, "전환 당일", steps);
    }

    private TransitionProtocol.PhaseTemplate advanceNextDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(new TransitionProtocol.StepTemplate(
                7, TransitionStepCategory.SLEEP, Duration.ofHours(37), Duration.ofHours(45),
                "새 근무 패턴에 맞춰 정규 취침 유지"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.NEXT_DAY, "전환 다음날", steps);
    }

    private TransitionProtocol delayProtocol(String protocolName) {
        List<TransitionProtocol.PhaseTemplate> phases = List.of(delayPrevDay(), delayDDay(), delayNextDay());
        return new TransitionProtocol(protocolName, "전환 전후 3일에 걸쳐 수면 시각을 늦추는 프로토콜입니다.", phases);
    }

    private TransitionProtocol.PhaseTemplate delayPrevDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(
                new TransitionProtocol.StepTemplate(
                        1, TransitionStepCategory.SLEEP, Duration.ofHours(-21), Duration.ofHours(-13),
                        "평소보다 2시간 늦게 취침, 늦게 기상"),
                new TransitionProtocol.StepTemplate(
                        2, TransitionStepCategory.LIGHT, Duration.ofHours(-26), Duration.ofMinutes(-25 * 60 - 30),
                        "저녁 밝은 빛 노출 30분"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.PREV_DAY, "전환 전날", steps);
    }

    private TransitionProtocol.PhaseTemplate delayDDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(
                new TransitionProtocol.StepTemplate(
                        3, TransitionStepCategory.NAP, Duration.ofHours(-7), Duration.ofHours(-6).minusMinutes(40),
                        "근무 전 낮잠으로 각성 보충"),
                new TransitionProtocol.StepTemplate(
                        4, TransitionStepCategory.MEAL, Duration.ofHours(-4), Duration.ofHours(-3),
                        "근무 전 든든한 식사"),
                new TransitionProtocol.StepTemplate(
                        5, TransitionStepCategory.CAFFEINE, Duration.ZERO, Duration.ofHours(2),
                        "근무 초반 카페인 섭취 허용"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.D_DAY, "전환 당일", steps);
    }

    private TransitionProtocol.PhaseTemplate delayNextDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(new TransitionProtocol.StepTemplate(
                6, TransitionStepCategory.SLEEP, Duration.ofHours(10), Duration.ofHours(18),
                "새 근무 패턴에 맞춰 주간 수면 유지"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.NEXT_DAY, "전환 다음날", steps);
    }
}
