package com.midtone.backend.chat.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ChatAnswerGeneratorConfig {

    @Bean
    public ChatAnswerGenerator chatAnswerGenerator(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.gemini.api-key:}") String geminiApiKey,
            @Value("${app.gemini.endpoint:https://generativelanguage.googleapis.com}") String geminiEndpoint,
            @Value("${app.gemini.model:gemini-3.5-flash-lite}") String geminiModel,
            @Value("${app.openai.api-key:}") String openAiApiKey,
            @Value("${app.openai.model:gpt-5.4-mini}") String openAiModel) {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            return new GeminiChatAnswerGenerator(
                    restClientBuilder, geminiApiKey, geminiEndpoint, geminiModel, objectMapper);
        }
        return new OpenAiChatAnswerGenerator(openAiApiKey, openAiModel, objectMapper);
    }
}
