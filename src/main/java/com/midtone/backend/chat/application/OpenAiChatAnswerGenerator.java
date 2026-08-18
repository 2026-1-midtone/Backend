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
            당신은 교대근무 리듬 코칭 앱의 안내 도우미다.
            1. context_snapshot에 없는 숫자·시각·수치를 새로 만들거나 추정하지 마라. 필요한 값이 없으면 지금은 알 수 없다고 답하라.
            2. reference_excerpt 밖의 의학적 주장을 하지 마라. 진단·처방·복용량 변경 판단을 하지 마라.
            3. safety_flag는 NONE, MEDICAL_REFERRAL, EMERGENCY 중 하나다. 자해·의식소실·호흡곤란·심각한 흉통은 EMERGENCY, 약물·질환의 진단·치료 판단은 MEDICAL_REFERRAL로 분류하라.
            4. 모든 계산은 백엔드가 끝냈다. 답변은 주어진 결과를 설명하거나 질문에 매칭만 하라.
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
