package com.midtone.backend.ocr.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.midtone.backend.ocr.application.OcrDraftParser;
import com.midtone.backend.shift.domain.ShiftType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class OpenAiOcrFallbackExtractorTest {

    private MockRestServiceServer server;
    private OpenAiOcrFallbackExtractor extractor;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        extractor = new OpenAiOcrFallbackExtractor(
                builder, new ObjectMapper(), true, "test-key", "gpt-test", "https://api.openai.com");
    }

    @Test
    void 이미지를_strict_구조화_출력으로_요청하고_검증된_초안을_반환한다() {
        byte[] image = new byte[] {1, 2, 3};
        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(image);
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-test"))
                .andExpect(jsonPath("$.store").value(false))
                .andExpect(jsonPath("$.input[0].content[0].text").value(
                        org.hamcrest.Matchers.containsString("2026-01")))
                .andExpect(jsonPath("$.input[0].content[1].image_url").value(dataUrl))
                .andExpect(jsonPath("$.input[0].content[1].detail").value("high"))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
                .andRespond(withSuccess(response("""
                        {"entries":[
                          {"day":1,"shift_type":"NIGHT"},
                          {"day":2,"shift_type":"OFF"}
                        ]}
                        """), MediaType.APPLICATION_JSON));

        List<OcrDraftParser.ParsedDraft> drafts =
                extractor.extract(image, "image/png", YearMonth.of(2026, 1));

        assertEquals(2, drafts.size());
        assertEquals(LocalDate.of(2026, 1, 1), drafts.get(0).workDate());
        assertEquals(ShiftType.NIGHT, drafts.get(0).shiftType());
        assertEquals(new BigDecimal("0.500"), drafts.get(0).confidence());
        assertEquals(LocalDate.of(2026, 1, 2), drafts.get(1).workDate());
        assertEquals(ShiftType.OFF, drafts.get(1).shiftType());
        server.verify();
    }

    @Test
    void 범위를_벗어난_날짜와_중복_날짜는_제외한다() {
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withSuccess(response("""
                        {"entries":[
                          {"day":1,"shift_type":"DAY"},
                          {"day":1,"shift_type":"NIGHT"},
                          {"day":32,"shift_type":"OFF"}
                        ]}
                        """), MediaType.APPLICATION_JSON));

        List<OcrDraftParser.ParsedDraft> drafts =
                extractor.extract(new byte[] {1}, "image/png", YearMonth.of(2026, 1));

        assertEquals(1, drafts.size());
        assertEquals(ShiftType.DAY, drafts.get(0).shiftType());
    }

    @Test
    void 날짜와_근무유형의_타입이_잘못된_항목은_제외한다() {
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withSuccess(response("""
                        {"entries":[
                          {"day":"1","shift_type":"DAY"},
                          {"day":2.5,"shift_type":"NIGHT"},
                          {"day":3,"shift_type":4},
                          {"day":4294967297,"shift_type":"DAY"},
                          {"day":4,"shift_type":"OFF"}
                        ]}
                        """), MediaType.APPLICATION_JSON));

        List<OcrDraftParser.ParsedDraft> drafts =
                extractor.extract(new byte[] {1}, "image/png", YearMonth.of(2026, 1));

        assertEquals(1, drafts.size());
        assertEquals(LocalDate.of(2026, 1, 4), drafts.get(0).workDate());
    }

    @Test
    void OCR_폴백이_비활성화되어_있으면_이미지를_전송하지_않는다() {
        OpenAiOcrFallbackExtractor disabled = new OpenAiOcrFallbackExtractor(
                RestClient.builder(), new ObjectMapper(), false,
                "test-key", "gpt-test", "https://api.openai.com");

        assertEquals(0, disabled.extract(new byte[] {1}, "image/png", YearMonth.of(2026, 1)).size());
    }

    private String response(String structuredText) {
        try {
            return new ObjectMapper().writeValueAsString(new Object[] {
                    java.util.Map.of("output", List.of(java.util.Map.of(
                            "type", "message",
                            "content", List.of(java.util.Map.of("type", "output_text", "text", structuredText)))))
            }).replaceFirst("^\\[", "").replaceFirst("]$", "");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
