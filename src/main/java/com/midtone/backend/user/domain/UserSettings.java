package com.midtone.backend.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    public static final int DEFAULT_PREFERRED_NAP_MINUTES = 20;
    public static final int DEFAULT_MAX_NAPS_PER_DAY = 2;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "caffeine_daily_mg")
    private Integer caffeineDailyMg;

    @Enumerated(EnumType.STRING)
    @Column(name = "caffeine_sensitivity", length = 20)
    private CaffeineSensitivity caffeineSensitivity;

    @Column(name = "preferred_nap_minutes", nullable = false)
    private Integer preferredNapMinutes;

    @Column(name = "max_naps_per_day", nullable = false)
    private Integer maxNapsPerDay;

    protected UserSettings() {
    }

    public UserSettings(Long userId, int preferredNapMinutes, int maxNapsPerDay) {
        this.userId = userId;
        this.preferredNapMinutes = preferredNapMinutes;
        this.maxNapsPerDay = maxNapsPerDay;
    }

    public Long getUserId() { return userId; }
    public Integer getCaffeineDailyMg() { return caffeineDailyMg; }
    public CaffeineSensitivity getCaffeineSensitivity() { return caffeineSensitivity; }
    public int getPreferredNapMinutes() { return preferredNapMinutes; }
    public int getMaxNapsPerDay() { return maxNapsPerDay; }

    public void changeCaffeineDailyMg(Integer caffeineDailyMg) {
        this.caffeineDailyMg = caffeineDailyMg;
    }

    public void changeCaffeineSensitivity(CaffeineSensitivity caffeineSensitivity) {
        this.caffeineSensitivity = caffeineSensitivity;
    }

    public void changePreferredNapMinutes(int preferredNapMinutes) {
        this.preferredNapMinutes = preferredNapMinutes;
    }

    public void changeMaxNapsPerDay(int maxNapsPerDay) {
        this.maxNapsPerDay = maxNapsPerDay;
    }
}
