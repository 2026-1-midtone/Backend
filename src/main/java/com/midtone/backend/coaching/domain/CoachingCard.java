package com.midtone.backend.coaching.domain;

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
@Table(name = "coaching_cards")
public class CoachingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "daily_coaching_id", nullable = false)
    private Long dailyCoachingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 30)
    private CoachingCardType cardType;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 500)
    private String rationale;

    protected CoachingCard() {
    }

    public CoachingCard(Long dailyCoachingId, CoachingCardContent content) {
        this.dailyCoachingId = dailyCoachingId;
        this.cardType = content.cardType();
        this.title = content.title();
        this.windowStart = content.windowStart();
        this.windowEnd = content.windowEnd();
        this.description = content.description();
        this.rationale = content.rationale();
    }

    public Long getId() { return id; }
    public Long getDailyCoachingId() { return dailyCoachingId; }
    public CoachingCardType getCardType() { return cardType; }
    public String getTitle() { return title; }
    public LocalDateTime getWindowStart() { return windowStart; }
    public LocalDateTime getWindowEnd() { return windowEnd; }
    public String getDescription() { return description; }
    public String getRationale() { return rationale; }

    public record CoachingCardContent(
            CoachingCardType cardType,
            String title,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            String description,
            String rationale) {
    }
}
