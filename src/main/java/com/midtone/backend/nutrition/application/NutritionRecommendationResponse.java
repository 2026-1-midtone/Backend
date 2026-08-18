package com.midtone.backend.nutrition.application;

import java.util.List;

public record NutritionRecommendationResponse(List<Recommendation> recommendations) {
    public record Recommendation(long productId, String productCode, String productName, String englishName,
            String imageUrl, List<String> matchedNutrients, List<FunctionInfo> functions, String disclaimer) {}
    public record FunctionInfo(String ingredient, String nutrientCode, String functionInfo) {}
}
