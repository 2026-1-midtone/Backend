package com.midtone.backend.nutrition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_nutrient_needs")
public class UserNutrientNeed {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(name = "nutrient_code", nullable = false, length = 50) private NutrientCode nutrientCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private NutrientNeedSource source;
    @Column(name = "recorded_on", nullable = false) private LocalDate recordedOn;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false) private LocalDateTime updatedAt;

    protected UserNutrientNeed() {}
    public UserNutrientNeed(long userId, NutrientCode nutrientCode, NutrientNeedSource source, LocalDate recordedOn) {
        this.userId = userId; this.nutrientCode = nutrientCode; this.source = source; this.recordedOn = recordedOn;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public NutrientCode getNutrientCode() { return nutrientCode; }
    public NutrientNeedSource getSource() { return source; }
    public LocalDate getRecordedOn() { return recordedOn; }
}
