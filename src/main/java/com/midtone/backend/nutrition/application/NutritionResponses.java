package com.midtone.backend.nutrition.application;

import java.time.OffsetDateTime;
import java.util.List;

public final class NutritionResponses {
    private NutritionResponses() {}
    public record PageInfo(int number, int size, long totalElements) {}
    public record Summary(long contentId, String contentType, String timingTag, String title,
                          String summary, String thumbnailUrl, boolean isFavorited) {}
    public record ListResponse(String recommendedTimingTag, List<Summary> contents, PageInfo page) {}
    public record Detail(long contentId, String contentType, String timingTag, String title, String summary,
                         String body, String sourceUrl, String disclaimer, boolean isFavorited) {}
    public record Favorite(long contentId, boolean isFavorited) {}
    public record FavoriteSummary(long contentId, String contentType, String timingTag, String title,
                                  String summary, String thumbnailUrl, OffsetDateTime favoritedAt) {}
    public record FavoritesResponse(List<FavoriteSummary> contents, PageInfo page) {}
}
