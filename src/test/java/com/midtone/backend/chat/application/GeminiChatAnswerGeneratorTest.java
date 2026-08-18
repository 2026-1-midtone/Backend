package com.midtone.backend.chat.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class GeminiChatAnswerGeneratorTest {

    private static final String EXPECTED_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private MockRestServiceServer server;
    private GeminiChatAnswerGenerator generator;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        generator = new GeminiChatAnswerGenerator(builder, "test-key",
                "https://generativelanguage.googleapis.com", "gemini-2.5-flash", new ObjectMapper());
    }

    private ChatPrompt prompt() {
        return new ChatPrompt("지금 커피 마셔도 돼?",
                ChatContextSnapshot.empty(LocalDate.parse("2026-08-19")), "카페인 근거", ChatDomain.CAFFEINE);
    }

    @Test
    void 시스템_프롬프트와_구조화_스키마로_호출하고_답변을_파싱한다() {
        String modelJson = "{\"answer_text\":\"컷오프 시각을 확인해 주세요.\",\"safety_flag\":\"NONE\",\"cited_domain\":\"CAFFEINE\"}";
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + toJsonString(modelJson) + "}]}}]}";
        server.expect(requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.system_instruction.parts[0].text",
                        org.hamcrest.Matchers.containsString("신뢰 경계")))
                .andExpect(jsonPath("$.contents[0].parts[0].text",
                        org.hamcrest.Matchers.containsString("지금 커피 마셔도 돼?")))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.properties.safety_flag.enum[0]").value("NONE"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeneratedChatAnswer answer = generator.generate(prompt());

        assertEquals("컷오프 시각을 확인해 주세요.", answer.answerText());
        assertEquals(SafetyFlag.NONE, answer.safetyFlag());
        assertEquals(ChatDomain.CAFFEINE, answer.citedDomain());
        server.verify();
    }

    @Test
    void cited_domain이_없어도_답변을_반환한다() {
        String modelJson = "{\"answer_text\":\"참고용 안내입니다.\",\"safety_flag\":\"NONE\"}";
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + toJsonString(modelJson) + "}]}}]}";
        server.expect(requestTo(EXPECTED_URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeneratedChatAnswer answer = generator.generate(prompt());

        assertNull(answer.citedDomain());
    }

    @Test
    void 서버_오류면_GENERATION_UNAVAILABLE_예외를_던진다() {
        server.expect(requestTo(EXPECTED_URL)).andRespond(withServerError());

        ChatException exception = assertThrows(ChatException.class, () -> generator.generate(prompt()));

        assertEquals(ChatException.ErrorCode.GENERATION_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void 키가_비어있으면_호출_없이_예외를_던진다() {
        GeminiChatAnswerGenerator withoutKey = new GeminiChatAnswerGenerator(RestClient.builder(), " ",
                "https://generativelanguage.googleapis.com", "gemini-2.5-flash", new ObjectMapper());

        ChatException exception = assertThrows(ChatException.class, () -> withoutKey.generate(prompt()));

        assertEquals(ChatException.ErrorCode.GENERATION_UNAVAILABLE, exception.getErrorCode());
    }

    private String toJsonString(String raw) {
        return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
