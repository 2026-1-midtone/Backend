package com.midtone.backend.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "preferred_nap_minutes", nullable = false)
    private Integer preferredNapMinutes;

    @Column(name = "max_naps_per_day", nullable = false)
    private Integer maxNapsPerDay;

    protected UserSettings() {
    }

    public int getPreferredNapMinutes() { return preferredNapMinutes; }
    public int getMaxNapsPerDay() { return maxNapsPerDay; }
}
