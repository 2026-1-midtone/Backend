package com.midtone.backend.chat.application;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class GeminiChatAnswerGenerator implements ChatAnswerGenerator {

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "answer_text", Map.of("type", "STRING"),
                    "safety_flag", Map.of("type", "STRING",
                            "enum", List.of("NONE", "MEDICAL_REFERRAL", "EMERGENCY")),
                    "cited_domain", Map.of("type", "STRING", "nullable", true,
                            "enum", List.of("CAFFEINE", "LIGHT", "NAP", "TRANSITION", "NUTRITION"))),
            "required", List.of("answer_text", "safety_flag"));

    private final RestClient restClient;
    private final String apiKey;
    private final String generateUrl;
    private final ObjectMapper objectMapper;

    public GeminiChatAnswerGenerator(RestClient.Builder restClientBuilder, String apiKey,
            String endpoint, String model, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.generateUrl = "%s/v1beta/models/%s:generateContent".formatted(endpoint, model);
        this.objectMapper = objectMapper;
    }

    @Override
    public GeneratedChatAnswer generate(ChatPrompt prompt) {
        if (apiKey == null || apiKey.isBlank()) throw new ChatException(ChatException.ErrorCode.GENERATION_UNAVAILABLE);
        try {
            JsonNode response = restClient.post()
                    .uri(generateUrl)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(prompt))
                    .retrieve()
                    .body(JsonNode.class);
            return parseAnswer(response);
        } catch (ChatException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ChatException(ChatException.ErrorCode.GENERATION_UNAVAILABLE);
        }
    }

    private Map<String, Object> requestBody(ChatPrompt prompt) {
        return Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", ChatPromptTexts.SYSTEM_PROMPT))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", ChatPromptTexts.renderInput(prompt))))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", RESPONSE_SCHEMA));
    }

    private GeneratedChatAnswer parseAnswer(JsonNode response) {
        JsonNode text = response == null
                ? null
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text == null || !text.isString()) {
            throw new ChatException(ChatException.ErrorCode.GENERATION_UNAVAILABLE);
        }
        StructuredAnswer answer = objectMapper.readValue(text.asString(), StructuredAnswer.class);
        if (answer.answer_text == null || answer.safety_flag == null) {
            throw new ChatException(ChatException.ErrorCode.GENERATION_UNAVAILABLE);
        }
        return new GeneratedChatAnswer(answer.answer_text, answer.safety_flag, answer.cited_domain);
    }

    public static class StructuredAnswer {
        public String answer_text;
        public SafetyFlag safety_flag;
        public ChatDomain cited_domain;
    }
}
