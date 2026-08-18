package com.midtone.backend.chat.application;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ChatReferenceCatalog {

    public ChatReference match(String question) {
        String value = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (contains(value, "커피", "카페인", "에너지드링크")) {
            return new ChatReference(ChatDomain.CAFFEINE,
                    "카페인 컷오프 시각은 백엔드가 계산한 값만 사용한다. 하루 섭취량이 2잔을 초과하면 잔 수 경고 상태로 본다. mg에서 잔 수를 추정하지 않는다.");
        }
        if (contains(value, "빛", "조명", "햇빛")) {
            return new ChatReference(ChatDomain.LIGHT,
                    "습관적 기상시각의 최근 7~14일 평균에서 약 2시간 전을 CBTmin 근사치로 사용한다. 실제 권장·지양 창은 백엔드 계산값을 설명만 한다.");
        }
        if (contains(value, "낮잠", "잠깐", "졸려")) {
            return new ChatReference(ChatDomain.NAP,
                    "낮잠 길이는 사용자 설정과 백엔드 계산값을 우선한다. 각성 후 특정 시간이 지나야 한다는 임계치는 근거가 확보되지 않아 새로 만들지 않는다.");
        }
        if (contains(value, "전환", "근무 바뀌", "데이", "나이트")) {
            return new ChatReference(ChatDomain.TRANSITION,
                    "근무 유형이 바뀌는 전환일에는 백엔드가 계산한 단계별 수면·빛·카페인 조정값만 설명한다.");
        }
        return new ChatReference(ChatDomain.NUTRITION,
                "영양 질문은 일반적인 참고 정보로만 답하고 질환 진단이나 개인 처방을 하지 않는다.");
    }

    private boolean contains(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
    }
}
