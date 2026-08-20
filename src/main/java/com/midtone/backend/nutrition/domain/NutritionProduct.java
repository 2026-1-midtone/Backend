package com.midtone.backend.nutrition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "nutrition_products")
public class NutritionProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "english_name", nullable = false, length = 200) private String englishName;
    @Column(name = "image_url", length = 2048) private String imageUrl;
    @Column(name = "product_url", length = 2048) private String productUrl;
    @Column(nullable = false, length = 500) private String disclaimer;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private LocalDateTime createdAt;

    protected NutritionProduct() {}
    public NutritionProduct(Long id, String code, String name, String englishName, String imageUrl,
            String productUrl, String disclaimer) {
        this.id = id; this.code = code; this.name = name; this.englishName = englishName;
        this.imageUrl = imageUrl; this.productUrl = productUrl; this.disclaimer = disclaimer;
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getEnglishName() { return englishName; }
    public String getImageUrl() { return imageUrl; }
    public String getProductUrl() { return productUrl; }
    public String getDisclaimer() { return disclaimer; }
}
