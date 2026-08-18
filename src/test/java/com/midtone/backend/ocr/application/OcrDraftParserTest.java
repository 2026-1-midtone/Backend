package com.midtone.backend.ocr.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.midtone.backend.shift.domain.ShiftType;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
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
}
