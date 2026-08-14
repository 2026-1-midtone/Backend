package com.midtone.backend.user.application.notification;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.user.domain.NotificationSetting;
import com.midtone.backend.user.domain.NotificationSettingRepository;
import com.midtone.backend.user.domain.NotificationType;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public NotificationSettingService(
            NotificationSettingRepository notificationSettingRepository,
            CurrentUserIdProvider currentUserIdProvider
    ) {
        this.notificationSettingRepository = notificationSettingRepository;
        this.currentUserIdProvider = currentUserIdProvider;
    }

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings() {
        long userId = currentUserIdProvider.getCurrentUserId();
        List<NotificationSettingResponse> settings = Arrays.stream(NotificationType.values())
                .map(type -> toResponse(userId, type))
                .toList();
        return new NotificationSettingsResponse(settings);
    }

    @Transactional
    public SaveNotificationSettingsResponse saveSettings(SaveNotificationSettingsRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        List<NotificationSettingSaveResponse> saved = request.settings().stream()
                .map(item -> saveOne(userId, item))
                .toList();
        return new SaveNotificationSettingsResponse(saved);
    }

    private NotificationSettingResponse toResponse(long userId, NotificationType type) {
        return notificationSettingRepository.findByUserIdAndType(userId, type)
                .map(setting -> NotificationSettingResponse.from(setting, type.getDefaultSuggestedTime()))
                .orElseGet(() -> NotificationSettingResponse.defaultFor(type));
    }

    private NotificationSettingSaveResponse saveOne(long userId, NotificationSettingRequestItem item) {
        NotificationType type = NotificationType.valueOf(item.type());
        NotificationSetting setting = findOrCreate(userId, type);
        setting.changeEnabled(item.enabled());
        setting.changeCustomTime(parseCustomTime(item.customTime()));
        notificationSettingRepository.save(setting);
        return NotificationSettingSaveResponse.from(setting);
    }

    private NotificationSetting findOrCreate(long userId, NotificationType type) {
        return notificationSettingRepository.findByUserIdAndType(userId, type)
                .orElseGet(() -> new NotificationSetting(userId, type, true, null));
    }

    private LocalTime parseCustomTime(String customTime) {
        return customTime == null ? null : LocalTime.parse(customTime);
    }
}
