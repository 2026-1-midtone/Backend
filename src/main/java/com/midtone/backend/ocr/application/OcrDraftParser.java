package com.midtone.backend.ocr.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.midtone.backend.shift.domain.ShiftType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class OcrDraftParser {

    public record ParsedDraft(LocalDate workDate, ShiftType shiftType, BigDecimal confidence) {
    }

    private static final Pattern DAY_ONLY = Pattern.compile("^(\\d{1,2})일?$");
    private static final Pattern MONTH_DAY = Pattern.compile("^(\\d{1,2})[/월.\\s]\\s*(\\d{1,2})일?$");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Map<String, ShiftType> SHIFT_CODES = Map.ofEntries(
            Map.entry("D", ShiftType.DAY), Map.entry("DAY", ShiftType.DAY),
            Map.entry("데이", ShiftType.DAY), Map.entry("주간", ShiftType.DAY),
            Map.entry("E", ShiftType.EVENING), Map.entry("EVE", ShiftType.EVENING),
            Map.entry("EVENING", ShiftType.EVENING), Map.entry("이브닝", ShiftType.EVENING),
            Map.entry("이브", ShiftType.EVENING),
            Map.entry("N", ShiftType.NIGHT), Map.entry("NIGHT", ShiftType.NIGHT),
            Map.entry("나이트", ShiftType.NIGHT), Map.entry("야간", ShiftType.NIGHT),
            Map.entry("OFF", ShiftType.OFF), Map.entry("O", ShiftType.OFF),
            Map.entry("X", ShiftType.OFF), Map.entry("휴", ShiftType.OFF),
            Map.entry("휴무", ShiftType.OFF), Map.entry("오프", ShiftType.OFF));

    public List<ParsedDraft> parse(JsonNode document, YearMonth targetMonth) {
        String fullText = document.path("text").asText("");
        Map<LocalDate, ParsedDraft> byDate = new LinkedHashMap<>();
        for (JsonNode page : document.path("pages")) {
            for (JsonNode table : page.path("tables")) {
                for (JsonNode row : table.path("bodyRows")) {
                    parseRow(row, fullText, targetMonth)
                            .ifPresent(draft -> byDate.putIfAbsent(draft.workDate(), draft));
                }
            }
        }
        List<ParsedDraft> drafts = new ArrayList<>(byDate.values());
        drafts.sort(Comparator.comparing(ParsedDraft::workDate));
        return drafts;
    }

    private Optional<ParsedDraft> parseRow(JsonNode row, String fullText, YearMonth targetMonth) {
        LocalDate workDate = null;
        int dateCellIndex = -1;
        JsonNode cells = row.path("cells");
        for (int i = 0; i < cells.size(); i++) {
            workDate = parseDate(cellText(cells.get(i), fullText), targetMonth);
            if (workDate != null) {
                dateCellIndex = i;
                break;
            }
        }
        if (workDate == null) {
            return Optional.empty();
        }
        for (int i = dateCellIndex + 1; i < cells.size(); i++) {
            JsonNode cell = cells.get(i);
            ShiftType shiftType = SHIFT_CODES.get(cellText(cell, fullText).toUpperCase());
            if (shiftType != null) {
                return Optional.of(new ParsedDraft(workDate, shiftType, cellConfidence(cell)));
            }
        }
        return Optional.empty();
    }

    private String cellText(JsonNode cell, String fullText) {
        StringBuilder text = new StringBuilder();
        for (JsonNode segment : cell.path("layout").path("textAnchor").path("textSegments")) {
            int start = segment.path("startIndex").asInt(0);
            int end = segment.path("endIndex").asInt(0);
            if (start >= 0 && end <= fullText.length() && start < end) {
                text.append(fullText, start, end);
            }
        }
        return text.toString().trim();
    }

    private BigDecimal cellConfidence(JsonNode cell) {
        double confidence = cell.path("layout").path("confidence").asDouble(0.0);
        return BigDecimal.valueOf(confidence).setScale(3, RoundingMode.HALF_UP);
    }

    private LocalDate parseDate(String text, YearMonth targetMonth) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text, ISO_DATE);
        } catch (DateTimeParseException ignored) {
        }
        Matcher monthDay = MONTH_DAY.matcher(text);
        if (monthDay.matches()) {
            return buildDate(
                    targetMonth.getYear(), Integer.parseInt(monthDay.group(1)), Integer.parseInt(monthDay.group(2)));
        }
        Matcher dayOnly = DAY_ONLY.matcher(text);
        if (dayOnly.matches()) {
            return buildDate(targetMonth.getYear(), targetMonth.getMonthValue(), Integer.parseInt(dayOnly.group(1)));
        }
        return null;
    }

    private LocalDate buildDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }
}
