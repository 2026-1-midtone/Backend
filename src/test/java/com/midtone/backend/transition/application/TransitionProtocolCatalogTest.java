package com.midtone.backend.transition.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.midtone.backend.shift.domain.ShiftType;
import org.junit.jupiter.api.Test;

class TransitionProtocolCatalogTest {

    private final TransitionProtocolCatalog transitionProtocolCatalog = new TransitionProtocolCatalog();

    @Test
    void 늦은_근무에서_이른_근무로_가면_수면을_앞당기는_프로토콜을_반환한다() {
        TransitionProtocol protocol = transitionProtocolCatalog.resolve(ShiftType.NIGHT, ShiftType.DAY);

        assertEquals("나이트 → 데이 전환", protocol.protocolName());
        assertTrue(protocol.description().contains("앞당기는"));
        assertEquals(3, protocol.phases().size());
    }

    @Test
    void 이른_근무에서_늦은_근무로_가면_수면을_늦추는_프로토콜을_반환한다() {
        TransitionProtocol protocol = transitionProtocolCatalog.resolve(ShiftType.DAY, ShiftType.NIGHT);

        assertEquals("데이 → 나이트 전환", protocol.protocolName());
        assertTrue(protocol.description().contains("늦추는"));
        assertEquals(3, protocol.phases().size());
    }

    @Test
    void 이브닝에서_나이트로_가면_수면을_늦추는_프로토콜을_반환한다() {
        TransitionProtocol protocol = transitionProtocolCatalog.resolve(ShiftType.EVENING, ShiftType.NIGHT);

        assertTrue(protocol.description().contains("늦추는"));
    }

    @Test
    void 나이트에서_이브닝으로_가면_수면을_앞당기는_프로토콜을_반환한다() {
        TransitionProtocol protocol = transitionProtocolCatalog.resolve(ShiftType.NIGHT, ShiftType.EVENING);

        assertTrue(protocol.description().contains("앞당기는"));
    }
}
