package com.midtone.backend.routine.application;

import com.midtone.backend.shift.domain.ShiftType;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 근무 유형별로 매일 되풀이되는 기본 루틴. 코칭 카드는 하루 최대 3장이고 전환일 스텝은 전환이 있는 날만
 * 생기기 때문에, 이것만으로는 평범한 근무일의 할 일 목록이 세 줄에서 끝난다.
 * 여기 있는 항목은 근무 시각만 있으면 언제나 만들 수 있는 것들로 채운다.
 *
 * <p>코칭 카드(빛 노출·낮잠·카페인 컷오프)와 겹치지 않도록 그 세 주제는 의도적으로 비워 두었다.
 */
@Component
public class BaselineRoutineCatalog {

    private static final List<Step> DAY = List.of(
            Step.fromStart("WAKE", "기상 후 물 한 잔 마시기", Duration.ofMinutes(-90), Duration.ofMinutes(-60)),
            Step.fromStart("LIGHT", "출근길에 햇빛 15분 쬐기", Duration.ofMinutes(-45), Duration.ZERO),
            Step.fromStart("MEAL", "점심은 거르지 말고 규칙적으로", Duration.ofHours(3), Duration.ofHours(4)),
            Step.fromEnd("HYDRATION", "오후에 물 한 잔 더 마시기", Duration.ofHours(-3), Duration.ofHours(-2)),
            Step.fromEnd("SLEEP", "잠들기 1시간 전 화면 끄기", Duration.ofHours(4), Duration.ofHours(5)));

    private static final List<Step> EVENING = List.of(
            Step.fromStart("WAKE", "기상 후 커튼 열고 햇빛 보기", Duration.ofHours(-3), Duration.ofMinutes(-150)),
            Step.fromStart("MEAL", "출근 전 든든한 식사", Duration.ofMinutes(-90), Duration.ofMinutes(-60)),
            Step.fromStart("HYDRATION", "근무 중 수분 보충하기", Duration.ofHours(4), Duration.ofHours(5)),
            Step.fromEnd("MEAL", "퇴근 후 야식은 가볍게", Duration.ZERO, Duration.ofMinutes(30)),
            Step.fromEnd("SLEEP", "잠들기 1시간 전 화면 끄기", Duration.ofHours(1), Duration.ofHours(2)));

    private static final List<Step> NIGHT = List.of(
            Step.fromStart("MEAL", "근무 전 든든한 식사", Duration.ofHours(-2), Duration.ofMinutes(-90)),
            Step.fromStart("MEAL", "새벽 간식은 가볍게", Duration.ofHours(4), Duration.ofHours(5)),
            Step.fromEnd("HYDRATION", "근무 후반 수분 보충하기", Duration.ofHours(-2), Duration.ofHours(-1)),
            Step.fromEnd("LIGHT", "퇴근길엔 선글라스로 빛 차단하기", Duration.ZERO, Duration.ofMinutes(30)),
            Step.fromEnd("SLEEP", "암막 커튼 치고 바로 취침하기", Duration.ofHours(1), Duration.ofHours(2)));

    private static final Map<ShiftType, List<Step>> BY_SHIFT_TYPE =
            Map.of(ShiftType.DAY, DAY, ShiftType.EVENING, EVENING, ShiftType.NIGHT, NIGHT);

    // 오프는 근무 앵커가 없으므로 절대 시각으로 잡는다. 쉬는 날에도 기상·취침 시각을 크게 흔들지 않는 것이 핵심.
    private static final List<OffStep> OFF = List.of(
            new OffStep("WAKE", "근무일과 비슷한 시각에 기상하기", LocalTime.of(8, 0), LocalTime.of(9, 0)),
            new OffStep("LIGHT", "아침 햇빛 20분 쬐기", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            new OffStep("HYDRATION", "물 충분히 마시기", LocalTime.of(14, 0), LocalTime.of(15, 0)),
            new OffStep("MEAL", "저녁은 취침 3시간 전까지", LocalTime.of(18, 0), LocalTime.of(19, 0)),
            new OffStep("SLEEP", "잠들기 1시간 전 화면 끄기", LocalTime.of(22, 0), LocalTime.of(23, 0)));

    public List<Step> resolve(ShiftType shiftType) {
        return BY_SHIFT_TYPE.getOrDefault(shiftType, List.of());
    }

    public List<OffStep> resolveOff() {
        return OFF;
    }

    public record Step(String category, String title, Anchor anchor, Duration startOffset, Duration endOffset) {

        public enum Anchor {
            SHIFT_START,
            SHIFT_END
        }

        static Step fromStart(String category, String title, Duration startOffset, Duration endOffset) {
            return new Step(category, title, Anchor.SHIFT_START, startOffset, endOffset);
        }

        static Step fromEnd(String category, String title, Duration startOffset, Duration endOffset) {
            return new Step(category, title, Anchor.SHIFT_END, startOffset, endOffset);
        }
    }

    public record OffStep(String category, String title, LocalTime windowStart, LocalTime windowEnd) {
    }
}
