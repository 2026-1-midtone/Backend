package com.midtone.backend.ocr.openai;

import com.midtone.backend.ocr.application.OcrDraftParser;
import com.midtone.backend.ocr.application.OcrFallbackExtractor;
import com.midtone.backend.shift.domain.ShiftType;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OpenAiOcrFallbackExtractor implements OcrFallbackExtractor {

    private static final BigDecimal FALLBACK_CONFIDENCE = new BigDecimal("0.500");
    private static final String INSTRUCTIONS = """
            Extract a shift schedule from the attached calendar image.
            Read only dates and shift marks visibly present in the image.
            Map D/데이/주간 to DAY, E/이브닝 to EVENING, N/나이트/야간 to NIGHT,
            and slash(/)/OFF/O/휴무 to OFF.
            Do not invent missing entries. Return each visible day at most once.
            """;

    public static class OcrFallbackException extends RuntimeException {
        public OcrFallbackException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final String responsesUrl;

    public OpenAiOcrFallbackExtractor(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.openai.ocr-enabled:false}") boolean enabled,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-5.4-mini}") String model,
            @Value("${app.openai.endpoint:https://api.openai.com}") String endpoint) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.responsesUrl = endpoint.replaceAll("/+$", "") + "/v1/responses";
    }

    @Override
    public List<OcrDraftParser.ParsedDraft> extract(byte[] image, String mimeType, YearMonth targetMonth) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        try {
            JsonNode response = restClient.post()
                    .uri(responsesUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(image, mimeType, targetMonth))
                    .retrieve()
                    .body(JsonNode.class);
            return parseResponse(response, targetMonth);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new OcrFallbackException("OpenAI OCR fallback 호출에 실패했습니다.", exception);
        }
    }

    private Map<String, Object> requestBody(byte[] image, String mimeType, YearMonth targetMonth) {
        String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(image);
        Map<String, Object> textPart = Map.of(
                "type", "input_text",
                "text", "target_month=" + targetMonth + "\nExtract every visible schedule entry for this month.");
        Map<String, Object> imagePart = Map.of(
                "type", "input_image",
                "image_url", dataUrl,
                "detail", "high");
        Map<String, Object> input = Map.of(
                "role", "user",
                "content", List.of(textPart, imagePart));
        Map<String, Object> format = Map.of(
                "type", "json_schema",
                "name", "shift_schedule",
                "strict", true,
                "schema", responseSchema());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", INSTRUCTIONS);
        body.put("store", false);
        body.put("input", List.of(input));
        body.put("text", Map.of("format", format));
        return body;
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> entrySchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "day", Map.of("type", "integer"),
                        "shift_type", Map.of(
                                "type", "string",
                                "enum", List.of("DAY", "EVENING", "NIGHT", "OFF"))),
                "required", List.of("day", "shift_type"),
                "additionalProperties", false);
        return Map.of(
                "type", "object",
                "properties", Map.of("entries", Map.of("type", "array", "items", entrySchema)),
                "required", List.of("entries"),
                "additionalProperties", false);
    }

    private List<OcrDraftParser.ParsedDraft> parseResponse(JsonNode response, YearMonth targetMonth) {
        String structuredText = findOutputText(response);
        if (structuredText == null) {
            return List.of();
        }
        JsonNode structured = objectMapper.readTree(structuredText);
        Map<LocalDate, OcrDraftParser.ParsedDraft> byDate = new LinkedHashMap<>();
        for (JsonNode entry : structured.path("entries")) {
            try {
                JsonNode dayNode = entry.path("day");
                JsonNode shiftTypeNode = entry.path("shift_type");
                if (!dayNode.isIntegralNumber() || !dayNode.canConvertToInt() || !shiftTypeNode.isTextual()) {
                    continue;
                }
                int day = dayNode.intValue();
                LocalDate date = targetMonth.atDay(day);
                ShiftType shiftType = ShiftType.valueOf(shiftTypeNode.asString());
                byDate.putIfAbsent(date, new OcrDraftParser.ParsedDraft(date, shiftType, FALLBACK_CONFIDENCE));
            } catch (DateTimeException | IllegalArgumentException ignored) {
                // 모델 출력은 스키마 외에도 백엔드 도메인 규칙으로 다시 검증한다.
            }
        }
        return new ArrayList<>(byDate.values());
    }

    private String findOutputText(JsonNode response) {
        if (response == null) {
            return null;
        }
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asString())) {
                    return content.path("text").asString(null);
                }
            }
        }
        return null;
    }
}
