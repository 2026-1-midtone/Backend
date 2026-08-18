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
        if (contains(value, "수면", "잤", "취침", "기상", "잠들")) {
            return new ChatReference(ChatDomain.SLEEP,
                    "수면 시간과 패턴은 context_snapshot의 값만 근거로 설명한다. recentSleepLogs에는 최근 수면 기록(취침 sleptAt, 기상 wokeAt, 수면 durationMinutes분)이 최신순으로 있고, sleepPattern에는 습관적 취침·기상·중간수면 시각이 있다. 특정 날짜의 수면을 물으면 recentSleepLogs에서 해당 기록을 찾아 시간으로 환산해 설명하고, 기록이 없으면 없다고 말한 뒤 수면 기록 등록을 안내한다. 수면장애 진단이나 수면제 관련 판단은 하지 않는다.");
        }
        return new ChatReference(ChatDomain.NUTRITION,
                "개인의 영양 결핍은 증상만으로 판단하지 않는다. context_snapshot에 사용자가 등록한 영양소 목표와 백엔드가 결정적으로 매칭한 제품 후보가 있을 때만 그 후보를 설명한다. 제품명, 기능정보, 매칭 영양소는 주어진 값을 그대로 사용하고 새로운 효능, 우선순위, 복용량을 만들지 않는다. 건강기능식품은 질병의 예방 및 치료를 위한 의약품이 아니다.");
    }

    public ChatReference productRecommendation() {
        return new ChatReference(ChatDomain.NUTRITION,
                """
                사용자가 '제품 추천받기' 버튼을 눌렀다. context_snapshot.nutritionRecommendations.recommendations의 \
                후보를 주어진 순서 그대로 모두 소개하라(매칭 영양소가 많은 제품이 먼저 정렬되어 있으며 순서를 바꾸지 않는다). \
                각 후보마다 제품명과 matchedNutrients(사용자가 등록한 영양소 목표와 매칭된 영양소), 그 영양소에 해당하는 \
                기능정보를 주어진 문구 그대로 한두 문장으로 요약한다. 후보에 없는 제품·원료·효능은 언급하지 않는다. \
                증상에서 결핍을 진단하거나 복용량·복용법·효과 기간을 만들지 않는다. 구매 권유나 과장 표현 없이 정보만 전달하고, \
                마지막에 건강기능식품은 의약품이 아니며 질병의 예방·치료를 위한 것이 아니라는 고지를 포함하라.""");
    }

    private boolean contains(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
    }
}
