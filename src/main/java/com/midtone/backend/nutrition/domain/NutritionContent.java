package com.midtone.backend.nutrition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "nutrition_contents")
public class NutritionContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NutritionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private NutritionContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "timing_tag", nullable = false, length = 30)
    private NutritionTimingTag timingTag;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(nullable = false, length = 500)
    private String disclaimer;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NutritionContent() {
    }

    public NutritionContent(NutritionCategory category, String title, String summary, String body) {
        this.category = category;
        this.title = title;
        this.summary = summary;
        this.body = body;
    }

    public Long getId() { return id; }
    public NutritionCategory getCategory() { return category; }
    public NutritionContentType getContentType() { return contentType; }
    public NutritionTimingTag getTimingTag() { return timingTag; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getBody() { return body; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getSourceUrl() { return sourceUrl; }
    public String getDisclaimer() { return disclaimer; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
