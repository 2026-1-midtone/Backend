package com.midtone.backend.user.application.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.user.domain.NotificationSetting;
import com.midtone.backend.user.domain.NotificationSettingRepository;
import com.midtone.backend.user.domain.NotificationType;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

    @Mock
    private NotificationSettingRepository notificationSettingRepository;
    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private NotificationSettingService notificationSettingService;

    @Test
    void 저장된_설정이_없으면_기본값과_제안시간을_반환한다() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(notificationSettingRepository.findByUserIdAndType(1L, NotificationType.NAP)).thenReturn(Optional.empty());
        when(notificationSettingRepository.findByUserIdAndType(1L, NotificationType.CAFFEINE_CUTOFF))
                .thenReturn(Optional.empty());
        when(notificationSettingRepository.findByUserIdAndType(1L, NotificationType.LIGHT_EXPOSURE))
                .thenReturn(Optional.empty());

        NotificationSettingsResponse response = notificationSettingService.getSettings();

        assertEquals(3, response.settings().size());
        NotificationSettingResponse nap = findByType(response.settings(), "NAP");
        assertEquals(true, nap.enabled());
        assertNull(nap.customTime());
        assertEquals("18:00", nap.suggestedTime());
    }

    @Test
    void 저장된_설정이_있으면_해당값과_제안시간을_반환한다() {
        NotificationSetting setting = new NotificationSetting(1L, NotificationType.CAFFEINE_CUTOFF, false, LocalTime.of(14, 30));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(notificationSettingRepository.findByUserIdAndType(1L, NotificationType.NAP)).thenReturn(Optional.empty());
        when(notificationSettingRepository.findByUserIdAndType(1L, NotificationType.CAFFEINE_CUTOFF))
                .thenReturn(Optional.of(setting));
        when(notificationSettingRepository.findByUserIdAndType(1L, NotificationType.LIGHT_EXPOSURE))
                .thenReturn(Optional.empty());

        NotificationSettingsResponse response = notificationSettingService.getSettings();

        NotificationSettingResponse caffeine = findByType(response.settings(), "CAFFEINE_CUTOFF");
        assertEquals(false, caffeine.enabled());
        assertEquals("14:30", caffeine.customTime());
        assertEquals("00:00", caffeine.suggestedTime());
    }

    @Test
    void 새로운_설정을_저장한다() {
        SaveNotificationSettingsRequest request = new SaveNotificationSettingsRequest(
                List.of(new NotificationSettingRequestItem("NAP", true, null)));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(notificationSettingRepository.findByUserIdAndType(1L, NotificationType.NAP)).thenReturn(Optional.empty());

        SaveNotificationSettingsResponse response = notificationSettingService.saveSettings(request);

        assertEquals(1, response.settings().size());
        assertEquals("NAP", response.settings().get(0).type());
        assertEquals(true, response.settings().get(0).enabled());
        assertNull(response.settings().get(0).customTime());
    }

    @Test
    void 기존_설정을_수정한다() {
        NotificationSetting existing = new NotificationSetting(1L, NotificationType.LIGHT_EXPOSURE, true, null);
        SaveNotificationSettingsRequest request = new SaveNotificationSettingsRequest(
                List.of(new NotificationSettingRequestItem("LIGHT_EXPOSURE", false, "21:30")));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(notificationSettingRepository.findByUserIdAndType(1L, NotificationType.LIGHT_EXPOSURE))
                .thenReturn(Optional.of(existing));

        SaveNotificationSettingsResponse response = notificationSettingService.saveSettings(request);

        assertEquals(false, response.settings().get(0).enabled());
        assertEquals("21:30", response.settings().get(0).customTime());
    }

    private NotificationSettingResponse findByType(List<NotificationSettingResponse> settings, String type) {
        return settings.stream()
                .filter(setting -> setting.type().equals(type))
                .findFirst()
                .orElseThrow();
    }
}
