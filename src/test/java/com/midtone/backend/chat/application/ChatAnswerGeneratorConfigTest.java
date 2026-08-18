package com.midtone.backend.chat.application;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class ChatAnswerGeneratorConfigTest {

    private final ChatAnswerGeneratorConfig config = new ChatAnswerGeneratorConfig();

    @Test
    void 제미나이_키가_있으면_Gemini_생성기를_사용한다() {
        ChatAnswerGenerator generator = config.chatAnswerGenerator(
                RestClient.builder(), new ObjectMapper(),
                "gemini-key", "https://generativelanguage.googleapis.com", "gemini-2.5-flash",
                "", "gpt-5.4-mini");

        assertInstanceOf(GeminiChatAnswerGenerator.class, generator);
    }

    @Test
    void 제미나이_키가_없으면_OpenAI_생성기를_사용한다() {
        ChatAnswerGenerator generator = config.chatAnswerGenerator(
                RestClient.builder(), new ObjectMapper(),
                " ", "https://generativelanguage.googleapis.com", "gemini-2.5-flash",
                "openai-key", "gpt-5.4-mini");

        assertInstanceOf(OpenAiChatAnswerGenerator.class, generator);
    }
}
