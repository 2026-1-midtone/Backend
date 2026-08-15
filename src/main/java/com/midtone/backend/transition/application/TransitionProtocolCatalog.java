package com.midtone.backend.transition.application;

import com.midtone.backend.shift.domain.ShiftType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

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
                        1, TransitionStepCategory.SLEEP, "08:00", "12:00", "퇴근 후 4시간만 앵커 수면"),
                new TransitionProtocol.StepTemplate(
                        2, TransitionStepCategory.LIGHT, "13:00", "14:00", "오후 야외 빛 노출 30분"),
                new TransitionProtocol.StepTemplate(
                        3, TransitionStepCategory.CAFFEINE, "00:00", "02:00", "카페인 섭취 중단"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.PREV_DAY, "전환 전날", steps);
    }

    private TransitionProtocol.PhaseTemplate advanceDDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(
                new TransitionProtocol.StepTemplate(
                        4, TransitionStepCategory.MEAL, "07:00", "08:00", "가벼운 아침 식사로 리듬 전환 신호주기"),
                new TransitionProtocol.StepTemplate(
                        5, TransitionStepCategory.LIGHT, "07:00", "08:00", "기상 후 아침 빛 노출 20분"),
                new TransitionProtocol.StepTemplate(
                        6, TransitionStepCategory.SLEEP, "22:00", "06:00", "목표 취침 시각에 맞춰 취침"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.D_DAY, "전환 당일", steps);
    }

    private TransitionProtocol.PhaseTemplate advanceNextDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(new TransitionProtocol.StepTemplate(
                7, TransitionStepCategory.SLEEP, "22:00", "06:00", "새 근무 패턴에 맞춰 정규 취침 유지"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.NEXT_DAY, "전환 다음날", steps);
    }

    private TransitionProtocol delayProtocol(String protocolName) {
        List<TransitionProtocol.PhaseTemplate> phases = List.of(delayPrevDay(), delayDDay(), delayNextDay());
        return new TransitionProtocol(protocolName, "전환 전후 3일에 걸쳐 수면 시각을 늦추는 프로토콜입니다.", phases);
    }

    private TransitionProtocol.PhaseTemplate delayPrevDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(
                new TransitionProtocol.StepTemplate(
                        1, TransitionStepCategory.SLEEP, "01:00", "09:00", "평소보다 2시간 늦게 취침, 늦게 기상"),
                new TransitionProtocol.StepTemplate(
                        2, TransitionStepCategory.LIGHT, "20:00", "21:00", "저녁 밝은 빛 노출 30분"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.PREV_DAY, "전환 전날", steps);
    }

    private TransitionProtocol.PhaseTemplate delayDDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(
                new TransitionProtocol.StepTemplate(
                        3, TransitionStepCategory.NAP, "15:00", "15:20", "근무 전 낮잠으로 각성 보충"),
                new TransitionProtocol.StepTemplate(
                        4, TransitionStepCategory.MEAL, "17:00", "18:00", "근무 전 든든한 식사"),
                new TransitionProtocol.StepTemplate(
                        5, TransitionStepCategory.CAFFEINE, "22:00", "24:00", "근무 초반 카페인 섭취 허용"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.D_DAY, "전환 당일", steps);
    }

    private TransitionProtocol.PhaseTemplate delayNextDay() {
        List<TransitionProtocol.StepTemplate> steps = List.of(new TransitionProtocol.StepTemplate(
                6, TransitionStepCategory.SLEEP, "08:00", "16:00", "새 근무 패턴에 맞춰 주간 수면 유지"));
        return new TransitionProtocol.PhaseTemplate(TransitionPhaseType.NEXT_DAY, "전환 다음날", steps);
    }
}
