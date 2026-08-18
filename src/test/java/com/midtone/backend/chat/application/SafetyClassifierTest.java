package com.midtone.backend.chat.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SafetyClassifierTest {

    private final SafetyClassifier safetyClassifier = new SafetyClassifier();

    @Test
    void 응급_키워드는_EMERGENCY로_분류한다() {
        assertEquals(SafetyClassifier.SafetyCategory.EMERGENCY,
                safetyClassifier.classify("가슴이 너무 아프고 숨쉬기 힘들어요"));
    }

    @Test
    void 의료_키워드는_MEDICAL로_분류한다() {
        assertEquals(SafetyClassifier.SafetyCategory.MEDICAL,
                safetyClassifier.classify("수면제 처방 받아야 할까요?"));
    }

    @Test
    void 응급이_의료보다_우선한다() {
        assertEquals(SafetyClassifier.SafetyCategory.EMERGENCY,
                safetyClassifier.classify("약물 복용 후 호흡곤란이 왔어요"));
    }

    @Test
    void 일반_질문은_NONE이다() {
        assertEquals(SafetyClassifier.SafetyCategory.NONE,
                safetyClassifier.classify("나이트 근무 전에 뭘 먹을까?"));
    }

    @Test
    void 공백을_제거하고_검사한다() {
        assertEquals(SafetyClassifier.SafetyCategory.EMERGENCY,
                safetyClassifier.classify("죽고 싶 다는 생각이 들어요"));
    }
}
