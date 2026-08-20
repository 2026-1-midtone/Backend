package com.midtone.backend.shift.domain;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 사용자가 근무 유형별 시각을 정하지 않았을 때 쓰는 대표값.
 *
 * <p>시작 시각은 {@code TransitionProtocolCatalog}의 단계별 오프셋이 기준으로 삼고 있는 값이라
 * 바꾸면 전환 프로토콜 계산이 함께 흔들린다. 값을 조정할 일이 있으면 그쪽 오프셋도 같이 봐야 한다.
 */
public final class ShiftTimeDefaults {

    private static final Map<ShiftType, ShiftTime> DEFAULTS = Map.of(
            ShiftType.DAY, new ShiftTime(LocalTime.of(9, 0), LocalTime.of(18, 0)),
            ShiftType.EVENING, new ShiftTime(LocalTime.of(12, 0), LocalTime.of(21, 0)),
            ShiftType.NIGHT, new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));

    /** 기본 시각을 정할 수 있는 유형. OFF 는 근무가 아니라 시각이 없다. */
    public static final List<ShiftType> CONFIGURABLE_TYPES = List.of(ShiftType.DAY, ShiftType.EVENING, ShiftType.NIGHT);

    private ShiftTimeDefaults() {
    }

    public static ShiftTime of(ShiftType shiftType) {
        return DEFAULTS.get(shiftType);
    }

    public static boolean isConfigurable(ShiftType shiftType) {
        return DEFAULTS.containsKey(shiftType);
    }
}
