package com.midtone.backend.chat.application;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SafetyClassifier {

    public enum SafetyCategory {
        EMERGENCY, MEDICAL, NONE
    }

    private static final List<String> EMERGENCY_KEYWORDS = List.of(
            "자살", "죽고싶", "가슴통증", "가슴이아프", "가슴이너무아프", "숨쉬기힘들", "호흡곤란",
            "쓰러졌", "쓰러질", "의식이없", "경련", "발작", "심하게어지럽", "응급", "119");

    private static final List<String> MEDICAL_KEYWORDS = List.of(
            "진단", "처방", "복용", "부작용", "수면제", "항우울제", "약물", "우울증", "불면증치료", "병원가야");

    public SafetyCategory classify(String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, EMERGENCY_KEYWORDS)) {
            return SafetyCategory.EMERGENCY;
        }
        if (containsAny(normalized, MEDICAL_KEYWORDS)) {
            return SafetyCategory.MEDICAL;
        }
        return SafetyCategory.NONE;
    }

    private String normalize(String message) {
        return message == null ? "" : message.replaceAll("\s+", "").toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String normalized, List<String> keywords) {
        return keywords.stream().anyMatch(normalized::contains);
    }
}
