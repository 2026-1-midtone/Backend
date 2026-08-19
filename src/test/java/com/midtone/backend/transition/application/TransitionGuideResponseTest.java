package com.midtone.backend.transition.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransitionGuideResponseTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 10);

    @Test
    void 실제_근무시작시각이_있으면_그것을_기준으로_단계_시각을_계산한다() {
        TransitionDetector.TransitionInfo info = new TransitionDetector.TransitionInfo(ShiftType.NIGHT, ShiftType.DAY);
        TransitionProtocol protocol = new TransitionProtocolCatalog().resolve(info.fromShiftType(), info.toShiftType());

        TransitionGuideResponse response = TransitionGuideResponse.of(DATE, info, protocol, LocalTime.of(8, 0));

        TransitionGuideResponse.Step sleepStep = dDaySleepStep(response);
        // D-Day 실제 근무시작 08:00 기준 +13h/+21h 오프셋 = 21:00 ~ 익일 05:00
        assertEquals(LocalDateTime.of(2026, 8, 10, 21, 0).toString(), sleepStep.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 11, 5, 0).toString(), sleepStep.windowEnd());
    }

    @Test
    void 실제_근무시작시각이_없으면_근무유형별_대표값으로_대체한다() {
        TransitionDetector.TransitionInfo info = new TransitionDetector.TransitionInfo(ShiftType.NIGHT, ShiftType.DAY);
        TransitionProtocol protocol = new TransitionProtocolCatalog().resolve(info.fromShiftType(), info.toShiftType());

        TransitionGuideResponse response = TransitionGuideResponse.of(DATE, info, protocol, null);

        TransitionGuideResponse.Step sleepStep = dDaySleepStep(response);
        // 대표값 DAY 09:00 기준 +13h/+21h 오프셋 = 22:00 ~ 익일 06:00
        assertEquals(LocalDateTime.of(2026, 8, 10, 22, 0).toString(), sleepStep.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 11, 6, 0).toString(), sleepStep.windowEnd());
    }

    @Test
    void resolveDDayShiftStart는_실제시각_우선_대표값_보조_순서로_계산한다() {
        assertEquals(
                LocalDateTime.of(2026, 8, 10, 8, 0),
                TransitionGuideResponse.resolveDDayShiftStart(DATE, ShiftType.DAY, LocalTime.of(8, 0)));
        assertEquals(
                LocalDateTime.of(2026, 8, 10, 22, 0),
                TransitionGuideResponse.resolveDDayShiftStart(DATE, ShiftType.NIGHT, null));
    }

    private TransitionGuideResponse.Step dDaySleepStep(TransitionGuideResponse response) {
        List<TransitionGuideResponse.Step> dDaySteps = response.phases().stream()
                .filter(phase -> phase.phase().equals(TransitionPhaseType.D_DAY.name()))
                .findFirst()
                .orElseThrow()
                .steps();
        return dDaySteps.stream()
                .filter(step -> step.category().equals("SLEEP"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void 전체_필드가_정상적으로_채워진다() {
        TransitionDetector.TransitionInfo info = new TransitionDetector.TransitionInfo(ShiftType.DAY, ShiftType.NIGHT);
        TransitionProtocol protocol = new TransitionProtocolCatalog().resolve(info.fromShiftType(), info.toShiftType());

        TransitionGuideResponse response = TransitionGuideResponse.of(DATE, info, protocol, LocalTime.of(22, 0));

        assertEquals(DATE.toString(), response.transitionDate());
        assertEquals("DAY", response.fromShiftType());
        assertEquals("NIGHT", response.toShiftType());
        assertTrue(response.protocolName().contains("나이트"));
        assertEquals(3, response.phases().size());
    }
}
