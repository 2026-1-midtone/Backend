package com.midtone.backend.nutrition.application;

import java.util.List;

public record NutritionProductCatalogResponse(List<Product> products) {
    public record Product(long productId, String productCode, String productName, String englishName,
                          String imageUrl, String productUrl,
                          List<NutritionRecommendationResponse.FunctionInfo> functions,
                          String disclaimer) {}
}
