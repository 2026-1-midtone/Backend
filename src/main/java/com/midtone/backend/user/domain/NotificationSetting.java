package com.midtone.backend.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "notification_settings")
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "custom_time")
    private LocalTime customTime;

    protected NotificationSetting() {
    }

    public NotificationSetting(Long userId, NotificationType type, boolean enabled, LocalTime customTime) {
        this.userId = userId;
        this.type = type;
        this.enabled = enabled;
        this.customTime = customTime;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public boolean isEnabled() { return enabled; }
    public LocalTime getCustomTime() { return customTime; }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void changeCustomTime(LocalTime customTime) {
        this.customTime = customTime;
    }
}
