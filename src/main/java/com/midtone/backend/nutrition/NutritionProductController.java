package com.midtone.backend.nutrition;

import com.midtone.backend.nutrition.application.NutritionProductCatalogResponse;
import com.midtone.backend.nutrition.application.NutritionRecommendationResponse;
import com.midtone.backend.nutrition.application.NutritionRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class NutritionProductController {
    private final NutritionRecommendationService service;
    public NutritionProductController(NutritionRecommendationService service) { this.service = service; }
    @GetMapping("/nutrition-products")
    public ResponseEntity<NutritionProductCatalogResponse> products() { return ResponseEntity.ok(service.getProducts()); }
    @GetMapping("/users/me/nutrition-product-recommendations")
    public ResponseEntity<NutritionRecommendationResponse> recommendations() {
        return ResponseEntity.ok(service.getRecommendations());
    }
}
