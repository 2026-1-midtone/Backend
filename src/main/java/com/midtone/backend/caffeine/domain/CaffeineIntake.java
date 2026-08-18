package com.midtone.backend.caffeine.domain;

import com.midtone.backend.global.time.DateTimeDefaults;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caffeine_intakes")
public class CaffeineIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "consumed_at", nullable = false)
    private LocalDateTime consumedAt;

    @Column(name = "recorded_timezone", nullable = false, length = 64)
    private String recordedTimezone;

    @Column(name = "amount_mg", nullable = false)
    private Integer amountMg;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal servings;

    @Column(name = "beverage_type", length = 50)
    private String beverageType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CaffeineIntake() {
    }

    public CaffeineIntake(long userId, LocalDateTime consumedAt, String recordedTimezone,
                          int amountMg, BigDecimal servings, String beverageType) {
        this.userId = userId;
        this.consumedAt = consumedAt;
        this.recordedTimezone = recordedTimezone;
        this.amountMg = amountMg;
        this.servings = servings;
        this.beverageType = beverageType;
        this.createdAt = LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE);
        this.updatedAt = this.createdAt;
    }

    public void update(LocalDateTime consumedAt, String recordedTimezone,
                       int amountMg, BigDecimal servings, String beverageType) {
        this.consumedAt = consumedAt;
        this.recordedTimezone = recordedTimezone;
        this.amountMg = amountMg;
        this.servings = servings;
        this.beverageType = beverageType;
        this.updatedAt = LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public String getRecordedTimezone() { return recordedTimezone; }
    public Integer getAmountMg() { return amountMg; }
    public BigDecimal getServings() { return servings; }
    public String getBeverageType() { return beverageType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
