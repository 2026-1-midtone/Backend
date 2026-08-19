package com.midtone.backend.ocr.application;

import tools.jackson.databind.JsonNode;
import com.midtone.backend.shift.domain.ShiftType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
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

    public record ParsedDraft(
            LocalDate workDate, ShiftType shiftType, BigDecimal confidence,
            LocalTime startTime, LocalTime endTime) {

        public ParsedDraft(LocalDate workDate, ShiftType shiftType, BigDecimal confidence) {
            this(workDate, shiftType, confidence, null, null);
        }
    }

    private static final Pattern DAY_ONLY = Pattern.compile("^(\\d{1,2})일?$");
    private static final Pattern TIME_RANGE = Pattern.compile("(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})");
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
            return parseCalendarTokens(document, fullText, targetMonth);
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
        int firstDayColumn = inferFirstDayColumn(tokens, columns, targetMonth);
        if (firstDayColumn < 0) {
            return List.of();
        }
        List<RowCluster> rows = calendarRows(tokens, columns, firstDayColumn);
        if (rows.size() < 3) {
            return List.of();
        }

        double rowSpacing = medianRowSpacing(rows);
        List<CalendarCandidate> candidates = new ArrayList<>();
        for (PositionedToken token : tokens) {
            String normalized = token.text().trim().toUpperCase();
            ShiftType shiftType = CALENDAR_SHIFT_CODES.get(normalized);
            if (shiftType == null) {
                continue;
            }
            int rowIndex = rowIndexAbove(rows, token.y(), rowSpacing);
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
                    TRUNCATED_KOREAN_SHIFT_CODES.contains(normalized) ? normalized : null,
                    token.y(), null, null));
        }
        for (TimeRangeToken timeToken : timeRangeTokens(tokens)) {
            int rowIndex = rowIndexAbove(rows, timeToken.y(), rowSpacing);
            int columnIndex = nearestColumnIndex(columns, timeToken.x());
            if (rowIndex < 0 || columnIndex < 0 || columnIndex > 6) {
                continue;
            }
            int day = rows.get(rowIndex).weekIndex() * 7 + columnIndex - firstDayColumn + 1;
            if (day < 1 || day > targetMonth.lengthOfMonth()) {
                continue;
            }
            candidates.add(new CalendarCandidate(
                    targetMonth.atDay(day),
                    shiftTypeForTimeRange(timeToken.startTime(), timeToken.endTime()),
                    timeToken.confidence(), null, timeToken.y(),
                    timeToken.startTime(), timeToken.endTime()));
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
            Map<ShiftType, CalendarCandidate> byType = new LinkedHashMap<>();
            for (List<CalendarCandidate> slot : badgeSlots(entry.getValue(), rowSpacing)) {
                if (slot.stream().map(CalendarCandidate::shiftType).distinct().count() != 1) {
                    continue;
                }
                CalendarCandidate strongest = slot.stream()
                        .max(Comparator.comparing(CalendarCandidate::confidence))
                        .orElseThrow();
                byType.merge(strongest.shiftType(), strongest, (first, second) ->
                        first.confidence().compareTo(second.confidence()) >= 0 ? first : second);
            }
            for (CalendarCandidate candidate : byType.values()) {
                drafts.add(new ParsedDraft(
                        entry.getKey(), candidate.shiftType(), candidate.confidence(),
                        candidate.startTime(), candidate.endTime()));
            }
        }
        drafts.sort(Comparator.comparing(ParsedDraft::workDate));
        return drafts;
    }

    private int inferFirstDayColumn(List<PositionedToken> tokens, List<Double> columns, YearMonth targetMonth) {
        int bestScore = 0;
        List<Integer> bestColumns = new ArrayList<>();
        for (int candidate = 0; candidate < 7; candidate++) {
            List<RowCluster> rows = calendarRows(tokens, columns, candidate);
            if (rows.size() < 3) {
                continue;
            }
            int score = rows.stream().mapToInt(RowCluster::voteCount).sum();
            if (score > bestScore) {
                bestScore = score;
                bestColumns.clear();
                bestColumns.add(candidate);
            } else if (score == bestScore) {
                bestColumns.add(candidate);
            }
        }
        if (bestColumns.size() == 1) {
            return bestColumns.get(0);
        }
        int naturalColumn = targetMonth.atDay(1).getDayOfWeek().getValue() % 7;
        return bestColumns.contains(naturalColumn) ? naturalColumn : -1;
    }

    private List<List<CalendarCandidate>> badgeSlots(List<CalendarCandidate> cellCandidates, double rowSpacing) {
        List<CalendarCandidate> ordered = new ArrayList<>(cellCandidates);
        ordered.sort(Comparator.comparingDouble(CalendarCandidate::y));
        List<List<CalendarCandidate>> slots = new ArrayList<>();
        for (CalendarCandidate candidate : ordered) {
            List<CalendarCandidate> lastSlot = slots.isEmpty() ? null : slots.get(slots.size() - 1);
            if (lastSlot == null
                    || candidate.y() - lastSlot.get(lastSlot.size() - 1).y() > rowSpacing * 0.20) {
                lastSlot = new ArrayList<>();
                slots.add(lastSlot);
            }
            lastSlot.add(candidate);
        }
        return slots;
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
        List<List<Double>> orderedClusters = clusters.stream()
                .sorted(Comparator.comparingDouble(cluster ->
                        cluster.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)))
                .toList();
        List<Double> centers = orderedClusters.stream()
                .map(cluster -> cluster.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                .toList();
        if (centers.size() <= 7) {
            return centers;
        }
        return selectEvenlySpacedColumns(orderedClusters, centers);
    }

    /**
     * 범례·미니 달력 등 달력 밖 숫자가 여덟 개 이상의 열 클러스터를 만들면,
     * 등간격으로 늘어선 일곱 열 조합 중 날짜 토큰 지지가 가장 큰 조합을 요일 열로 선택한다.
     */
    private List<Double> selectEvenlySpacedColumns(List<List<Double>> orderedClusters, List<Double> centers) {
        List<Double> best = List.of();
        int bestSupport = 0;
        for (int left = 0; left < centers.size() - 1; left++) {
            for (int right = left + 1; right < centers.size(); right++) {
                double spacing = (centers.get(right) - centers.get(left)) / 6.0;
                if (spacing < 0.04) {
                    continue;
                }
                List<Double> selected = new ArrayList<>();
                int support = 0;
                for (int position = 0; position < 7; position++) {
                    double expected = centers.get(left) + position * spacing;
                    int matched = -1;
                    double matchedDistance = Double.MAX_VALUE;
                    for (int i = 0; i < centers.size(); i++) {
                        double distance = Math.abs(centers.get(i) - expected);
                        if (distance < matchedDistance) {
                            matched = i;
                            matchedDistance = distance;
                        }
                    }
                    if (matchedDistance > spacing * 0.25) {
                        selected = null;
                        break;
                    }
                    selected.add(centers.get(matched));
                    support += orderedClusters.get(matched).size();
                }
                if (selected != null && support > bestSupport) {
                    bestSupport = support;
                    best = List.copyOf(selected);
                }
            }
        }
        return best;
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
            List<PositionedToken> tokens, List<Double> columns, int firstDayColumn) {
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
            inferCalendarRow(cluster, columns, firstDayColumn)
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
            MutableRowCluster cluster, List<Double> columns, int firstDayColumn) {
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

    private int rowIndexAbove(List<RowCluster> rows, double tokenY, double rowSpacing) {
        int above = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (tokenY >= rows.get(i).y() - rowSpacing * 0.20) {
                above = i;
            }
        }
        if (above < 0) {
            return -1;
        }
        return tokenY - rows.get(above).y() <= rowSpacing * 0.95 ? above : -1;
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
            LocalDate workDate, ShiftType shiftType, BigDecimal confidence, String truncatedCode, double y,
            LocalTime startTime, LocalTime endTime) {
    }

    private record TimeRangeToken(LocalTime startTime, LocalTime endTime, double x, double y, BigDecimal confidence) {
    }

    /**
     * OCR이 "09:00-18:00"을 여러 토큰으로 쪼개도 같은 줄의 토큰을 이어붙여 시간대 배지를 복원한다.
     */
    private List<TimeRangeToken> timeRangeTokens(List<PositionedToken> tokens) {
        List<List<PositionedToken>> lines = new ArrayList<>();
        for (PositionedToken token : tokens.stream()
                .sorted(Comparator.comparingDouble(PositionedToken::y))
                .toList()) {
            List<PositionedToken> line = lines.isEmpty() ? null : lines.get(lines.size() - 1);
            if (line == null
                    || Math.abs(line.stream().mapToDouble(PositionedToken::y).average().orElse(0.0) - token.y())
                    > 0.008) {
                line = new ArrayList<>();
                lines.add(line);
            }
            line.add(token);
        }
        List<TimeRangeToken> result = new ArrayList<>();
        for (List<PositionedToken> line : lines) {
            line.sort(Comparator.comparingDouble(PositionedToken::x));
            StringBuilder text = new StringBuilder();
            List<Integer> charToToken = new ArrayList<>();
            for (int i = 0; i < line.size(); i++) {
                String tokenText = line.get(i).text().trim();
                for (int c = 0; c < tokenText.length(); c++) {
                    charToToken.add(i);
                }
                text.append(tokenText);
            }
            Matcher matcher = TIME_RANGE.matcher(text);
            while (matcher.find()) {
                LocalTime start = parseTime(matcher.group(1), matcher.group(2));
                LocalTime end = parseTime(matcher.group(3), matcher.group(4));
                if (start == null || end == null || start.equals(end)) {
                    continue;
                }
                double x = 0.0;
                double y = 0.0;
                BigDecimal confidence = null;
                int tokenCount = 0;
                int previousToken = -1;
                for (int c = matcher.start(); c < matcher.end(); c++) {
                    int tokenIndex = charToToken.get(c);
                    if (tokenIndex == previousToken) {
                        continue;
                    }
                    previousToken = tokenIndex;
                    PositionedToken token = line.get(tokenIndex);
                    x += token.x();
                    y += token.y();
                    confidence = confidence == null || token.confidence().compareTo(confidence) < 0
                            ? token.confidence()
                            : confidence;
                    tokenCount++;
                }
                result.add(new TimeRangeToken(start, end, x / tokenCount, y / tokenCount, confidence));
            }
        }
        return result;
    }

    private LocalTime parseTime(String hour, String minute) {
        int hourValue = Integer.parseInt(hour);
        int minuteValue = Integer.parseInt(minute);
        if (hourValue > 23 || minuteValue > 59) {
            return null;
        }
        return LocalTime.of(hourValue, minuteValue);
    }

    private ShiftType shiftTypeForTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime) || startTime.getHour() >= 20) {
            return ShiftType.NIGHT;
        }
        if (startTime.getHour() >= 11) {
            return ShiftType.EVENING;
        }
        return ShiftType.DAY;
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
