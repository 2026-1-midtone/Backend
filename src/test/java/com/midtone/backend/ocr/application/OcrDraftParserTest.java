package com.midtone.backend.ocr.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.midtone.backend.shift.domain.ShiftType;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OcrDraftParserTest {

    private final OcrDraftParser parser = new OcrDraftParser();
    private JsonNode document;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ocr/form-parser-response.json")) {
            document = new ObjectMapper().readTree(in);
        }
    }

    @Test
    void 표의_각_행을_날짜와_근무유형_초안으로_변환한다() {
        List<OcrDraftParser.ParsedDraft> drafts = parser.parse(document, YearMonth.of(2026, 8));

        assertEquals(4, drafts.size());
        assertEquals(LocalDate.of(2026, 8, 1), drafts.get(0).workDate());
        assertEquals(ShiftType.DAY, drafts.get(0).shiftType());
        assertEquals(LocalDate.of(2026, 8, 2), drafts.get(1).workDate());
        assertEquals(ShiftType.NIGHT, drafts.get(1).shiftType());
        assertEquals(LocalDate.of(2026, 8, 3), drafts.get(2).workDate());
        assertEquals(ShiftType.OFF, drafts.get(2).shiftType());
        assertEquals(LocalDate.of(2026, 8, 4), drafts.get(3).workDate());
        assertEquals(ShiftType.EVENING, drafts.get(3).shiftType());
    }

    @Test
    void 한글_데이와_나이트_표기도_근무유형으로_변환한다() throws Exception {
        JsonNode koreanShiftDocument = new ObjectMapper().readTree("""
                {
                  "text": "1 데이 2 나이트",
                  "pages": [{
                    "tables": [{
                      "bodyRows": [
                        {"cells": [
                          {"layout": {"textAnchor": {"textSegments": [{"startIndex": "0", "endIndex": "1"}]}}},
                          {"layout": {"textAnchor": {"textSegments": [{"startIndex": "2", "endIndex": "4"}]}, "confidence": 0.98}}
                        ]},
                        {"cells": [
                          {"layout": {"textAnchor": {"textSegments": [{"startIndex": "5", "endIndex": "6"}]}}},
                          {"layout": {"textAnchor": {"textSegments": [{"startIndex": "7", "endIndex": "10"}]}, "confidence": 0.96}}
                        ]}
                      ]
                    }]
                  }]
                }
                """);

        List<OcrDraftParser.ParsedDraft> drafts = parser.parse(koreanShiftDocument, YearMonth.of(2026, 8));

        assertEquals(2, drafts.size());
        assertEquals(LocalDate.of(2026, 8, 1), drafts.get(0).workDate());
        assertEquals(ShiftType.DAY, drafts.get(0).shiftType());
        assertEquals(new BigDecimal("0.980"), drafts.get(0).confidence());
        assertEquals(LocalDate.of(2026, 8, 2), drafts.get(1).workDate());
        assertEquals(ShiftType.NIGHT, drafts.get(1).shiftType());
        assertEquals(new BigDecimal("0.960"), drafts.get(1).confidence());
    }

    @Test
    void 달력_토큰의_좌표로_날짜와_근무유형을_연결한다() throws Exception {
        List<TokenSpec> tokens = new ArrayList<>();
        double[] rowY = {0.098, 0.249, 0.400, 0.552, 0.705, 0.856};
        int[][] visibleDays = {
                {26, 27, 28, 29, 30, 31, 1},
                {2, 3, 4, 5, 6, 7, 8},
                {9, 10, 11, 12, 13, 14, 15},
                {16, 17, 1, 13, 19, 20, 21},
                {23, 24, 25, 12, 26, 27, 28},
                {30, 31, 1, 2, 3, 4, 5}
        };
        for (int row = 0; row < visibleDays.length; row++) {
            for (int column = 0; column < 7; column++) {
                tokens.add(new TokenSpec(Integer.toString(visibleDays[row][column]),
                        0.081 + column * 0.135, rowY[row], 0.95));
            }
        }
        tokens.add(new TokenSpec("8", 0.160, 0.025, 0.99));
        tokens.addAll(List.of(
                new TokenSpec("쉬는", 0.160, 0.579, 0.968),
                new TokenSpec("광복절", 0.188, 0.579, 0.952),
                new TokenSpec("오프", 0.296, 0.578, 0.960),
                new TokenSpec("나이트", 0.434, 0.579, 0.980),
                new TokenSpec("데이", 0.565, 0.579, 0.970),
                new TokenSpec("이브닝", 0.704, 0.579, 0.917),
                new TokenSpec("나이트", 0.839, 0.579, 0.976),
                new TokenSpec("데이", 0.160, 0.731, 0.970),
                new TokenSpec("데이", 0.295, 0.732, 0.969),
                new TokenSpec("나이트", 0.434, 0.731, 0.984),
                new TokenSpec("오프", 0.565, 0.731, 0.955),
                new TokenSpec("나이트", 0.704, 0.731, 0.979)));

        List<OcrDraftParser.ParsedDraft> drafts =
                parser.parse(calendarDocument(tokens), YearMonth.of(2026, 8));

        assertEquals(10, drafts.size());
        assertDraft(drafts.get(0), 18, ShiftType.OFF);
        assertDraft(drafts.get(1), 19, ShiftType.NIGHT);
        assertDraft(drafts.get(2), 20, ShiftType.DAY);
        assertDraft(drafts.get(3), 21, ShiftType.EVENING);
        assertDraft(drafts.get(4), 22, ShiftType.NIGHT);
        assertDraft(drafts.get(5), 24, ShiftType.DAY);
        assertDraft(drafts.get(6), 25, ShiftType.DAY);
        assertDraft(drafts.get(7), 26, ShiftType.NIGHT);
        assertDraft(drafts.get(8), 27, ShiftType.OFF);
        assertDraft(drafts.get(9), 28, ShiftType.NIGHT);
        assertEquals(new BigDecimal("0.960"), drafts.get(0).confidence());
    }

    @Test
    void 달력에서_잘린_한글_근무명도_해당_날짜에_연결한다() throws Exception {
        List<TokenSpec> tokens = new ArrayList<>();
        double[] rowY = {0.200, 0.350, 0.500, 0.650, 0.800};
        int[][] visibleDays = {
                {30, 31, 1, 2, 3, 4, 5},
                {6, 7, 8, 9, 10, 11, 12},
                {13, 14, 15, 16, 17, 18, 19},
                {20, 21, 22, 23, 24, 25, 26},
                {27, 28, 29, 30, 1, 2, 3}
        };
        for (int row = 0; row < visibleDays.length; row++) {
            for (int column = 0; column < 7; column++) {
                tokens.add(new TokenSpec(Integer.toString(visibleDays[row][column]),
                        0.080 + column * 0.135, rowY[row], 0.95));
            }
        }
        tokens.addAll(List.of(
                new TokenSpec("오", 0.620, 0.230, 0.96),
                new TokenSpec("데", 0.755, 0.230, 0.96),
                new TokenSpec("데", 0.485, 0.380, 0.96),
                new TokenSpec("오", 0.620, 0.380, 0.96)));

        List<OcrDraftParser.ParsedDraft> drafts =
                parser.parse(calendarDocument(tokens), YearMonth.of(2026, 9));

        assertEquals(4, drafts.size());
        assertDraftForMonth(drafts.get(0), 2026, 9, 3, ShiftType.OFF);
        assertDraftForMonth(drafts.get(1), 2026, 9, 4, ShiftType.DAY);
        assertDraftForMonth(drafts.get(2), 2026, 9, 9, ShiftType.DAY);
        assertDraftForMonth(drafts.get(3), 2026, 9, 10, ShiftType.OFF);
    }

    @Test
    void 첫째_주가_누락되어도_인식된_날짜의_합의로_주차를_계산한다() throws Exception {
        List<TokenSpec> tokens = new ArrayList<>();
        double[] rowY = {0.350, 0.500, 0.650, 0.800};
        int day = 2;
        for (double y : rowY) {
            for (int column = 0; column < 7 && day <= 29; column++, day++) {
                tokens.add(new TokenSpec(Integer.toString(day), 0.080 + column * 0.135, y, 0.95));
            }
        }
        tokens.add(new TokenSpec("나이트", 0.080, 0.380, 0.98));

        List<OcrDraftParser.ParsedDraft> drafts =
                parser.parse(calendarDocument(tokens), YearMonth.of(2026, 8));

        assertEquals(1, drafts.size());
        assertDraft(drafts.get(0), 2, ShiftType.NIGHT);
    }

    @Test
    void 당월_날짜가_하나뿐인_첫째_주도_인접_주차로_확정한다() throws Exception {
        List<TokenSpec> tokens = new ArrayList<>();
        int[][] visibleDays = {
                {26, 27, 28, 29, 30, 31, 1},
                {2, 3, 4, 5, 6, 7, 8},
                {9, 10, 11, 12, 13, 14, 15},
                {16, 17, 18, 19, 20, 21, 22}
        };
        for (int row = 0; row < visibleDays.length; row++) {
            for (int column = 0; column < 7; column++) {
                tokens.add(new TokenSpec(Integer.toString(visibleDays[row][column]),
                        0.080 + column * 0.135, 0.200 + row * 0.150, 0.95));
            }
        }
        tokens.add(new TokenSpec("나이트", 0.890, 0.230, 0.98));

        List<OcrDraftParser.ParsedDraft> drafts =
                parser.parse(calendarDocument(tokens), YearMonth.of(2026, 8));

        assertEquals(1, drafts.size());
        assertDraft(drafts.get(0), 1, ShiftType.NIGHT);
    }

    @Test
    void 중간_달력_행의_추론이_실패하면_멀리_떨어진_약한_주차를_승인하지_않는다() throws Exception {
        List<TokenSpec> tokens = new ArrayList<>();
        for (int column = 0; column < 7; column++) {
            tokens.add(new TokenSpec("2", 0.080 + column * 0.135, 0.200, 0.95));
        }
        tokens.add(new TokenSpec("3", 0.215, 0.350, 0.95));
        for (int column = 0; column < 7; column++) {
            tokens.add(new TokenSpec(Integer.toString(9 + column),
                    0.080 + column * 0.135, 0.500, 0.95));
            tokens.add(new TokenSpec(Integer.toString(16 + column),
                    0.080 + column * 0.135, 0.650, 0.95));
        }
        tokens.add(new TokenSpec("나이트", 0.080, 0.230, 0.98));

        assertEquals(0, parser.parse(calendarDocument(tokens), YearMonth.of(2026, 8)).size());
    }

    @Test
    void 달력_아래의_다음달_숫자를_이전_주차로_역방향_연결하지_않는다() throws Exception {
        List<TokenSpec> tokens = completeCalendarTokens(YearMonth.of(2026, 8));
        tokens.add(new TokenSpec("3", 0.215, 0.920, 0.97));
        tokens.add(new TokenSpec("나이트", 0.080, 0.380, 0.98));

        List<OcrDraftParser.ParsedDraft> drafts =
                parser.parse(calendarDocument(tokens), YearMonth.of(2026, 8));

        assertEquals(1, drafts.size());
        assertDraft(drafts.get(0), 2, ShiftType.NIGHT);
    }

    @Test
    void 일곱_요일_열을_확정할_수_없는_달력은_거부한다() throws Exception {
        List<TokenSpec> tokens = new ArrayList<>();
        for (int row = 0; row < 4; row++) {
            for (int column = 1; column < 7; column++) {
                int day = row * 7 + column - 5;
                if (day >= 1 && day <= 31) {
                    tokens.add(new TokenSpec(Integer.toString(day), 0.080 + column * 0.135,
                            0.200 + row * 0.150, 0.95));
                }
            }
        }
        tokens.add(new TokenSpec("데이", 0.350, 0.380, 0.98));

        assertEquals(0, parser.parse(calendarDocument(tokens), YearMonth.of(2026, 8)).size());
    }

    @Test
    void 달력의_단독_한글_한_글자는_근무로_인식하지_않는다() throws Exception {
        List<TokenSpec> tokens = completeCalendarTokens(YearMonth.of(2026, 8));
        tokens.add(new TokenSpec("휴", 0.080, 0.230, 0.98));
        tokens.add(new TokenSpec("이", 0.215, 0.380, 0.98));
        tokens.add(new TokenSpec("나", 0.350, 0.530, 0.98));
        tokens.add(new TokenSpec("오", 0.485, 0.680, 0.98));
        tokens.add(new TokenSpec("데", 0.620, 0.680, 0.98));

        assertEquals(0, parser.parse(calendarDocument(tokens), YearMonth.of(2026, 8)).size());
    }

    @Test
    void 같은_달력_셀에서_근무유형이_충돌하면_해당_셀을_제외한다() throws Exception {
        List<TokenSpec> tokens = completeCalendarTokens(YearMonth.of(2026, 8));
        tokens.add(new TokenSpec("데이", 0.080, 0.380, 0.98));
        tokens.add(new TokenSpec("나이트", 0.081, 0.381, 0.97));

        assertEquals(0, parser.parse(calendarDocument(tokens), YearMonth.of(2026, 8)).size());
    }

    @Test
    void 근무_셀의_confidence를_초안에_기록한다() {
        List<OcrDraftParser.ParsedDraft> drafts = parser.parse(document, YearMonth.of(2026, 8));
        assertEquals(new BigDecimal("0.970"), drafts.get(0).confidence());
    }

    @Test
    void 날짜나_근무유형을_해석하지_못한_행은_제외한다() {
        // 픽스처 마지막 행("?", "X")에서 "?"는 날짜가 아니므로 초안 4건만 생성된다
        List<OcrDraftParser.ParsedDraft> drafts = parser.parse(document, YearMonth.of(2026, 8));
        assertEquals(4, drafts.size());
    }

    @Test
    void 일_숫자는_targetMonth와_결합한다() {
        // ISO 날짜 행(2026-08-04)은 그대로, 일 숫자 행(1,2,3)은 2026-09와 결합되어 정렬된다
        List<OcrDraftParser.ParsedDraft> drafts = parser.parse(document, YearMonth.of(2026, 9));
        assertEquals(LocalDate.of(2026, 8, 4), drafts.get(0).workDate());
        assertEquals(LocalDate.of(2026, 9, 1), drafts.get(1).workDate());
    }

    @Test
    void 표가_없으면_빈_목록을_반환한다() throws Exception {
        JsonNode empty = new ObjectMapper().readTree("{\"text\":\"\",\"pages\":[]}");
        assertEquals(0, parser.parse(empty, YearMonth.of(2026, 8)).size());
    }

    private JsonNode calendarDocument(List<TokenSpec> tokenSpecs) throws Exception {
        StringBuilder fullText = new StringBuilder();
        List<Map<String, Object>> tokens = new ArrayList<>();
        for (TokenSpec spec : tokenSpecs) {
            int start = fullText.length();
            fullText.append(spec.text()).append('\n');
            int end = start + spec.text().length();
            Map<String, Object> layout = new LinkedHashMap<>();
            layout.put("textAnchor", Map.of("textSegments", List.of(Map.of(
                    "startIndex", Integer.toString(start), "endIndex", Integer.toString(end)))));
            layout.put("confidence", spec.confidence());
            layout.put("boundingPoly", Map.of("normalizedVertices", List.of(
                    Map.of("x", spec.x() - 0.004, "y", spec.y() - 0.004),
                    Map.of("x", spec.x() + 0.004, "y", spec.y() - 0.004),
                    Map.of("x", spec.x() + 0.004, "y", spec.y() + 0.004),
                    Map.of("x", spec.x() - 0.004, "y", spec.y() + 0.004))));
            tokens.add(Map.of("layout", layout));
        }
        Map<String, Object> document = Map.of(
                "text", fullText.toString(),
                "pages", List.of(Map.of("tokens", tokens, "tables", List.of())));
        return new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(document));
    }

    private List<TokenSpec> completeCalendarTokens(YearMonth month) {
        List<TokenSpec> tokens = new ArrayList<>();
        int firstDayColumn = month.atDay(1).getDayOfWeek().getValue() % 7;
        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 7; column++) {
                int day = row * 7 + column - firstDayColumn + 1;
                if (day >= 1 && day <= month.lengthOfMonth()) {
                    tokens.add(new TokenSpec(Integer.toString(day), 0.080 + column * 0.135,
                            0.200 + row * 0.150, 0.95));
                }
            }
        }
        return tokens;
    }

    private void assertDraft(OcrDraftParser.ParsedDraft draft, int day, ShiftType shiftType) {
        assertDraftForMonth(draft, 2026, 8, day, shiftType);
    }

    private void assertDraftForMonth(
            OcrDraftParser.ParsedDraft draft, int year, int month, int day, ShiftType shiftType) {
        assertEquals(LocalDate.of(year, month, day), draft.workDate());
        assertEquals(shiftType, draft.shiftType());
    }

    private record TokenSpec(String text, double x, double y, double confidence) {
    }
}
