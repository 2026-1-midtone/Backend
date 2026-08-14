package com.midtone.backend.user.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByUserIdAndType(long userId, NotificationType type);
}
