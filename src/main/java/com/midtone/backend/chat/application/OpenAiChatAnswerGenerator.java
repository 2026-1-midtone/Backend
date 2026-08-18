package com.midtone.backend.chat.application;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OpenAiChatAnswerGenerator implements ChatAnswerGenerator {

    static final String SYSTEM_PROMPT = """
            당신은 교대근무 간호사를 위한 리듬 코칭 앱의 채팅 도우미다. 사용자의 근무 일정·수면·카페인·영양 데이터를 \
            바탕으로, 백엔드가 이미 계산해 둔 결과를 친절하고 정확하게 설명한다.

            [신뢰 경계 — 가장 중요한 규칙]
            - 사실의 유일한 출처는 입력의 context_snapshot과 reference_excerpt다.
            - context_snapshot에 없는 숫자·시각·수치·일정·제품을 새로 만들거나 추정하지 마라. 필요한 값이 없으면 \
            "지금은 확인할 수 없어요"라고 답하고, 값을 채우려면 무엇을 기록해야 하는지 안내하라.
            - 모든 계산은 백엔드가 끝냈다. 당신의 역할은 주어진 결과를 질문에 맞게 골라 설명하는 것뿐이다.
            - reference_excerpt 밖의 의학적 주장을 하지 마라. 진단·처방·복용량 판단은 어떤 경우에도 하지 마라.

            [안전 분류]
            - safety_flag는 NONE, MEDICAL_REFERRAL, EMERGENCY 중 하나다.
            - 자해·자살 암시, 의식 소실, 호흡곤란, 심한 흉통 등 응급 신호 → EMERGENCY로 분류하고 답변 대신 즉시 \
            119·응급실 도움을 요청하도록 안내하라.
            - 약물·질환의 진단, 치료, 복용 여부·용량 판단 요청 → MEDICAL_REFERRAL로 분류하고 의사·약사 상담을 권하라.

            [영양 제품 규칙]
            - 제품은 context_snapshot.nutritionRecommendations.recommendations에 있는 후보만, 주어진 순서 그대로 언급하라.
            - matchedNutrients는 사용자가 직접 등록한 영양소 목표와의 매칭 결과다. 증상으로 영양소 결핍을 진단하지 마라.
            - functions의 기능정보 문구는 식약처 고시 표현이므로 그대로 사용하고 확장·과장하지 마라. \
            복용량·복용법·복용 시점·효과 기간을 만들지 마라.
            - 제품을 언급한 답변의 끝에는 건강기능식품은 의약품이 아니며 질병의 예방·치료를 위한 것이 아니라는 점을 밝혀라.

            [답변 스타일]
            - 한국어 존댓말로, 서론 없이 핵심부터 말하라. 답변은 2~5문장 또는 짧은 목록으로 간결하게 유지하라.
            - 사용자의 데이터를 인용할 때는 어떤 값을 근거로 했는지 함께 말하라. \
            (예: "오늘 나이트 근무가 22시에 시작되니…")
            - cited_domain에는 실제 답변의 근거로 사용한 도메인 하나만 지정하라.
            - 확신이 없는 내용은 단정하지 말고 참고용 정보임을 밝혀라.
            """;

    private final String model;
    private final ObjectMapper objectMapper;
    private final OpenAIClient client;

    public OpenAiChatAnswerGenerator(@Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-5.4-mini}") String model, ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.client = apiKey == null || apiKey.isBlank() ? null
                : OpenAIOkHttpClient.builder().apiKey(apiKey).build();
    }

    @Override
    public GeneratedChatAnswer generate(ChatPrompt prompt) {
        if (client == null) throw new ChatException(ChatException.ErrorCode.GENERATION_UNAVAILABLE);
        try {
            String input = "question:\n" + prompt.question()
                    + "\n\ncontext_snapshot:\n" + objectMapper.writeValueAsString(prompt.contextSnapshot())
                    + "\n\nreference_excerpt(" + prompt.domain().name() + "):\n" + prompt.referenceExcerpt();
            StructuredResponseCreateParams<StructuredAnswer> params = ResponseCreateParams.builder()
                    .instructions(SYSTEM_PROMPT)
                    .input(input)
                    .text(StructuredAnswer.class)
                    .model(model)
                    .build();
            StructuredAnswer answer = client.responses().create(params).output().stream()
                    .flatMap(item -> item.message().stream())
                    .flatMap(message -> message.content().stream())
                    .flatMap(content -> content.outputText().stream())
                    .findFirst()
                    .orElseThrow(() -> new ChatException(ChatException.ErrorCode.GENERATION_UNAVAILABLE));
            return new GeneratedChatAnswer(Objects.requireNonNull(answer.answer_text),
                    Objects.requireNonNull(answer.safety_flag),
                    answer.cited_domain.orElse(null));
        } catch (ChatException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ChatException(ChatException.ErrorCode.GENERATION_UNAVAILABLE);
        }
    }

    public static class StructuredAnswer {
        public String answer_text;
        public SafetyFlag safety_flag;
        public Optional<ChatDomain> cited_domain;
    }
}
