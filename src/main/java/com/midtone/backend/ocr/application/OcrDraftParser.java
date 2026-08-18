package com.midtone.backend.ocr.application;

import tools.jackson.databind.JsonNode;
import com.midtone.backend.shift.domain.ShiftType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    private static final Map<String, ShiftType> CALENDAR_SHIFT_CODES = Map.ofEntries(
            Map.entry("D", ShiftType.DAY), Map.entry("DAY", ShiftType.DAY),
            Map.entry("데이", ShiftType.DAY), Map.entry("데", ShiftType.DAY),
            Map.entry("주간", ShiftType.DAY),
            Map.entry("E", ShiftType.EVENING), Map.entry("EVE", ShiftType.EVENING),
            Map.entry("EVENING", ShiftType.EVENING), Map.entry("이브닝", ShiftType.EVENING),
            Map.entry("이브", ShiftType.EVENING),
            Map.entry("N", ShiftType.NIGHT), Map.entry("NIGHT", ShiftType.NIGHT),
            Map.entry("나이트", ShiftType.NIGHT),
            Map.entry("야간", ShiftType.NIGHT),
            Map.entry("OFF", ShiftType.OFF), Map.entry("O", ShiftType.OFF),
            Map.entry("X", ShiftType.OFF), Map.entry("/", ShiftType.OFF),
            Map.entry("휴무", ShiftType.OFF),
            Map.entry("오프", ShiftType.OFF), Map.entry("오", ShiftType.OFF));
    private static final Set<String> TRUNCATED_KOREAN_SHIFT_CODES = Set.of("데", "오");

    public List<ParsedDraft> parse(JsonNode document, YearMonth targetMonth) {
        String fullText = document.path("text").asString("");
        Map<LocalDate, ParsedDraft> byDate = new LinkedHashMap<>();
        for (JsonNode page : document.path("pages")) {
            for (JsonNode table : page.path("tables")) {
                for (JsonNode row : table.path("bodyRows")) {
                    parseRow(row, fullText, targetMonth)
                            .ifPresent(draft -> byDate.putIfAbsent(draft.workDate(), draft));
                }
            }
        }
        if (byDate.isEmpty()) {
            parseCalendarTokens(document, fullText, targetMonth)
                    .forEach(draft -> byDate.putIfAbsent(draft.workDate(), draft));
        }
        List<ParsedDraft> drafts = new ArrayList<>(byDate.values());
        drafts.sort(Comparator.comparing(ParsedDraft::workDate));
        return drafts;
    }

    private List<ParsedDraft> parseCalendarTokens(JsonNode document, String fullText, YearMonth targetMonth) {
        List<PositionedToken> tokens = positionedTokens(document, fullText);
        List<Double> columns = calendarColumns(tokens);
        if (columns.size() != 7) {
            return List.of();
        }
        List<RowCluster> rows = calendarRows(tokens, columns, targetMonth);
        if (rows.size() < 3) {
            return List.of();
        }

        double rowSpacing = medianRowSpacing(rows);
        int firstDayColumn = targetMonth.atDay(1).getDayOfWeek().getValue() % DayOfWeek.values().length;
        List<CalendarCandidate> candidates = new ArrayList<>();
        for (PositionedToken token : tokens) {
            String normalized = token.text().trim().toUpperCase();
            ShiftType shiftType = CALENDAR_SHIFT_CODES.get(normalized);
            if (shiftType == null) {
                continue;
            }
            int rowIndex = nearestRowIndex(rows, token.y(), rowSpacing);
            int columnIndex = nearestColumnIndex(columns, token.x());
            if (rowIndex < 0 || columnIndex < 0 || columnIndex > 6) {
                continue;
            }
            int day = rows.get(rowIndex).weekIndex() * 7 + columnIndex - firstDayColumn + 1;
            if (day < 1 || day > targetMonth.lengthOfMonth()) {
                continue;
            }
            LocalDate date = targetMonth.atDay(day);
            candidates.add(new CalendarCandidate(
                    date, shiftType, token.confidence(),
                    TRUNCATED_KOREAN_SHIFT_CODES.contains(normalized) ? normalized : null));
        }
        Map<String, Long> truncatedCounts = new HashMap<>();
        for (CalendarCandidate candidate : candidates) {
            if (candidate.truncatedCode() != null) {
                truncatedCounts.merge(candidate.truncatedCode(), 1L, Long::sum);
            }
        }
        Map<LocalDate, List<CalendarCandidate>> byDate = new LinkedHashMap<>();
        for (CalendarCandidate candidate : candidates) {
            if (candidate.truncatedCode() != null
                    && truncatedCounts.getOrDefault(candidate.truncatedCode(), 0L) < 2) {
                continue;
            }
            byDate.computeIfAbsent(candidate.workDate(), ignored -> new ArrayList<>()).add(candidate);
        }
        List<ParsedDraft> drafts = new ArrayList<>();
        for (Map.Entry<LocalDate, List<CalendarCandidate>> entry : byDate.entrySet()) {
            List<CalendarCandidate> cellCandidates = entry.getValue();
            if (cellCandidates.stream().map(CalendarCandidate::shiftType).distinct().count() != 1) {
                continue;
            }
            CalendarCandidate strongest = cellCandidates.stream()
                    .max(Comparator.comparing(CalendarCandidate::confidence))
                    .orElseThrow();
            drafts.add(new ParsedDraft(entry.getKey(), strongest.shiftType(), strongest.confidence()));
        }
        return drafts;
    }

    private List<Double> calendarColumns(List<PositionedToken> tokens) {
        List<PositionedToken> dateTokens = tokens.stream()
                .filter(token -> isCalendarDay(token.text()))
                .filter(token -> token.y() > 0.05 && token.y() < 0.95)
                .sorted(Comparator.comparingDouble(PositionedToken::x))
                .toList();
        List<List<Double>> clusters = new ArrayList<>();
        for (PositionedToken token : dateTokens) {
            List<Double> cluster = clusters.stream()
                    .filter(candidate -> Math.abs(candidate.stream().mapToDouble(Double::doubleValue)
                            .average().orElse(0.0) - token.x()) <= 0.025)
                    .findFirst()
                    .orElseGet(() -> {
                        List<Double> created = new ArrayList<>();
                        clusters.add(created);
                        return created;
                    });
            cluster.add(token.x());
        }
        return clusters.stream()
                .map(cluster -> cluster.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                .sorted()
                .toList();
    }

    private List<PositionedToken> positionedTokens(JsonNode document, String fullText) {
        List<PositionedToken> tokens = new ArrayList<>();
        for (JsonNode page : document.path("pages")) {
            for (JsonNode token : page.path("tokens")) {
                JsonNode layout = token.path("layout");
                JsonNode vertices = layout.path("boundingPoly").path("normalizedVertices");
                if (vertices.isEmpty()) {
                    continue;
                }
                double x = 0.0;
                double y = 0.0;
                for (JsonNode vertex : vertices) {
                    x += vertex.path("x").asDouble(0.0);
                    y += vertex.path("y").asDouble(0.0);
                }
                tokens.add(new PositionedToken(
                        cellText(token, fullText).trim(),
                        x / vertices.size(),
                        y / vertices.size(),
                        cellConfidence(token)));
            }
        }
        return tokens;
    }

    private List<RowCluster> calendarRows(
            List<PositionedToken> tokens, List<Double> columns, YearMonth targetMonth) {
        List<PositionedToken> dateTokens = tokens.stream()
                .filter(token -> isCalendarDay(token.text()))
                .filter(token -> token.y() > 0.05 && token.y() < 0.95)
                .sorted(Comparator.comparingDouble(PositionedToken::y))
                .toList();
        List<MutableRowCluster> clusters = new ArrayList<>();
        for (PositionedToken token : dateTokens) {
            MutableRowCluster cluster = clusters.stream()
                    .filter(candidate -> Math.abs(candidate.averageY() - token.y()) <= 0.012)
                    .findFirst()
                    .orElseGet(() -> {
                        MutableRowCluster created = new MutableRowCluster();
                        clusters.add(created);
                        return created;
                    });
            cluster.add(token);
        }
        List<MutableRowCluster> orderedClusters = clusters.stream()
                .sorted(Comparator.comparingDouble(MutableRowCluster::averageY))
                .toList();
        List<RowCluster> inferred = new ArrayList<>();
        for (MutableRowCluster cluster : orderedClusters) {
            inferCalendarRow(cluster, columns, targetMonth)
                    .ifPresent(inferred::add);
        }
        double weekHeight = inferredWeekHeight(inferred);
        if (weekHeight <= 0.0) {
            return List.of();
        }
        List<RowCluster> accepted = new ArrayList<>();
        for (RowCluster row : inferred) {
            boolean supported = row.voteCount() >= 2;
            if (!supported) {
                for (RowCluster neighbor : inferred) {
                    int weekDelta = row.weekIndex() - neighbor.weekIndex();
                    double yDelta = row.y() - neighbor.y();
                    if (neighbor.voteCount() >= 2
                            && weekDelta != 0
                            && Math.abs(yDelta - weekDelta * weekHeight)
                            <= weekHeight * 0.20) {
                        supported = true;
                        break;
                    }
                }
            }
            if (supported) {
                accepted.add(row);
            }
        }
        for (int i = 1; i < accepted.size(); i++) {
            RowCluster previous = accepted.get(i - 1);
            RowCluster current = accepted.get(i);
            int weekDistance = current.weekIndex() - previous.weekIndex();
            if (weekDistance <= 0
                    || Math.abs((current.y() - previous.y()) - weekDistance * weekHeight)
                    > weekHeight * 0.20) {
                return List.of();
            }
        }
        return accepted;
    }

    private Optional<RowCluster> inferCalendarRow(
            MutableRowCluster cluster, List<Double> columns, YearMonth targetMonth) {
        int firstDayColumn = targetMonth.atDay(1).getDayOfWeek().getValue() % 7;
        Map<Integer, Integer> votes = new HashMap<>();
        for (PositionedToken token : cluster.tokens) {
            int column = nearestColumnIndex(columns, token.x());
            int day = Integer.parseInt(token.text());
            int numerator = day - 1 + firstDayColumn - column;
            if (column >= 0 && Math.floorMod(numerator, 7) == 0) {
                int weekIndex = numerator / 7;
                if (weekIndex >= 0 && weekIndex <= 5) {
                    votes.merge(weekIndex, 1, Integer::sum);
                }
            }
        }
        int bestVotes = votes.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Integer> winners = votes.entrySet().stream()
                .filter(entry -> entry.getValue() == bestVotes)
                .map(Map.Entry::getKey)
                .toList();
        if (bestVotes < 1 || winners.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(new RowCluster(
                cluster.averageY(), winners.get(0), bestVotes, List.copyOf(cluster.tokens)));
    }

    private double inferredWeekHeight(List<RowCluster> rows) {
        List<Double> estimates = new ArrayList<>();
        List<RowCluster> strongRows = rows.stream().filter(row -> row.voteCount() >= 2).toList();
        for (int i = 0; i < strongRows.size(); i++) {
            for (int j = i + 1; j < strongRows.size(); j++) {
                RowCluster first = strongRows.get(i);
                RowCluster second = strongRows.get(j);
                int weekDistance = second.weekIndex() - first.weekIndex();
                if (weekDistance > 0) {
                    estimates.add((second.y() - first.y()) / weekDistance);
                }
            }
        }
        if (estimates.isEmpty()) {
            return 0.0;
        }
        estimates.sort(Double::compareTo);
        return estimates.get(estimates.size() / 2);
    }

    private boolean isCalendarDay(String text) {
        if (!text.matches("\\d{1,2}")) {
            return false;
        }
        int value = Integer.parseInt(text);
        return value >= 1 && value <= 31;
    }

    private double medianRowSpacing(List<RowCluster> rows) {
        List<Double> spacings = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            RowCluster previous = rows.get(i - 1);
            RowCluster current = rows.get(i);
            spacings.add((current.y() - previous.y()) / (current.weekIndex() - previous.weekIndex()));
        }
        spacings.sort(Double::compareTo);
        return spacings.get(spacings.size() / 2);
    }

    private int nearestRowIndex(List<RowCluster> rows, double tokenY, double rowSpacing) {
        int nearest = -1;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < rows.size(); i++) {
            double distance = Math.abs(rows.get(i).y() - tokenY);
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        return nearestDistance <= rowSpacing * 0.45 ? nearest : -1;
    }

    private int nearestColumnIndex(List<Double> columns, double tokenX) {
        int nearest = -1;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < columns.size(); i++) {
            double distance = Math.abs(columns.get(i) - tokenX);
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        double typicalSpacing = columns.size() < 2 ? 0.0 : (columns.get(6) - columns.get(0)) / 6.0;
        return nearestDistance <= typicalSpacing * 0.45 ? nearest : -1;
    }

    private record PositionedToken(String text, double x, double y, BigDecimal confidence) {
    }

    private record RowCluster(double y, int weekIndex, int voteCount, List<PositionedToken> tokens) {
    }

    private record CalendarCandidate(
            LocalDate workDate, ShiftType shiftType, BigDecimal confidence, String truncatedCode) {
    }

    private static class MutableRowCluster {
        private final List<PositionedToken> tokens = new ArrayList<>();

        private void add(PositionedToken token) {
            tokens.add(token);
        }

        private double averageY() {
            return tokens.stream().mapToDouble(PositionedToken::y).average().orElse(0.0);
        }
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
