package com.midtone.backend.chat.application;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.Objects;
import java.util.Optional;
import tools.jackson.databind.ObjectMapper;

public class OpenAiChatAnswerGenerator implements ChatAnswerGenerator {

    static final String SYSTEM_PROMPT = ChatPromptTexts.SYSTEM_PROMPT;

    private final String model;
    private final ObjectMapper objectMapper;
    private final OpenAIClient client;

    public OpenAiChatAnswerGenerator(String apiKey, String model, ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.client = apiKey == null || apiKey.isBlank() ? null
                : OpenAIOkHttpClient.builder().apiKey(apiKey).build();
    }

    @Override
    public GeneratedChatAnswer generate(ChatPrompt prompt) {
        if (client == null) throw new ChatException(ChatException.ErrorCode.GENERATION_UNAVAILABLE);
        try {
            String input = ChatPromptTexts.renderInput(prompt);
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
