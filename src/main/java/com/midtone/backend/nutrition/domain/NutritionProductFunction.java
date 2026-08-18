package com.midtone.backend.nutrition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nutrition_product_functions")
public class NutritionProductFunction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(nullable = false, length = 100) private String ingredient;
    @Enumerated(EnumType.STRING) @Column(name = "nutrient_code", length = 50) private NutrientCode nutrientCode;
    @Column(name = "function_info", nullable = false, length = 500) private String functionInfo;
    @Column(name = "sort_order", nullable = false) private Integer sortOrder;

    protected NutritionProductFunction() {}
    public NutritionProductFunction(long productId, String ingredient, NutrientCode nutrientCode,
            String functionInfo, int sortOrder) {
        this.productId = productId; this.ingredient = ingredient; this.nutrientCode = nutrientCode;
        this.functionInfo = functionInfo; this.sortOrder = sortOrder;
    }
    public Long getProductId() { return productId; }
    public String getIngredient() { return ingredient; }
    public NutrientCode getNutrientCode() { return nutrientCode; }
    public String getFunctionInfo() { return functionInfo; }
    public Integer getSortOrder() { return sortOrder; }
}
