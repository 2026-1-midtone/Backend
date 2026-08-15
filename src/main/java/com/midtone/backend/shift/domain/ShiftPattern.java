package com.midtone.backend.shift.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shift_patterns")
public class ShiftPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "cycle_days", nullable = false)
    private int cycleDays;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shift_pattern_items", joinColumns = @JoinColumn(name = "pattern_id"))
    @OrderColumn(name = "day_index")
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false, length = 20)
    private List<ShiftType> pattern = new ArrayList<>();

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ShiftPattern() {
    }

    public ShiftPattern(Long userId, String name, List<ShiftType> pattern) {
        this.userId = userId;
        this.name = name;
        this.pattern = new ArrayList<>(pattern);
        this.cycleDays = pattern.size();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public int getCycleDays() { return cycleDays; }
    public List<ShiftType> getPattern() { return pattern; }
}
