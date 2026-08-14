package com.midtone.backend.user.application.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.user.domain.CaffeineSensitivity;
import com.midtone.backend.user.domain.UserSettings;
import com.midtone.backend.user.domain.UserSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;
    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private UserSettingsService userSettingsService;

    @Test
    void 저장된_설정이_없으면_기본값을_반환한다() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.empty());

        SettingsResponse response = userSettingsService.getSettings();

        assertEquals(UserSettings.DEFAULT_PREFERRED_NAP_MINUTES, response.preferredNapMinutes());
        assertEquals(UserSettings.DEFAULT_MAX_NAPS_PER_DAY, response.maxNapsPerDay());
        assertNull(response.caffeineDailyMg());
        assertNull(response.caffeineSensitivity());
    }

    @Test
    void 저장된_설정이_있으면_그대로_반환한다() {
        UserSettings settings = new UserSettings(1L, 25, 3);
        settings.changeCaffeineDailyMg(200);
        settings.changeCaffeineSensitivity(CaffeineSensitivity.HIGH);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        SettingsResponse response = userSettingsService.getSettings();

        assertEquals(25, response.preferredNapMinutes());
        assertEquals(3, response.maxNapsPerDay());
        assertEquals(200, response.caffeineDailyMg());
        assertEquals("HIGH", response.caffeineSensitivity());
    }

    @Test
    void 기존_설정이_없으면_새로_생성해서_저장한다() {
        SaveSettingsRequest request = new SaveSettingsRequest(300, "HIGH", 20, 2);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.empty());

        SettingsResponse response = userSettingsService.saveSettings(request);

        assertEquals(300, response.caffeineDailyMg());
        assertEquals("HIGH", response.caffeineSensitivity());
        assertEquals(20, response.preferredNapMinutes());
        assertEquals(2, response.maxNapsPerDay());
    }

    @Test
    void 기존_설정이_있으면_값을_갱신한다() {
        UserSettings existing = new UserSettings(1L, 20, 2);
        SaveSettingsRequest request = new SaveSettingsRequest(null, "LOW", 30, 1);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(userSettingsRepository.findById(1L)).thenReturn(Optional.of(existing));

        SettingsResponse response = userSettingsService.saveSettings(request);

        assertEquals(30, response.preferredNapMinutes());
        assertEquals(1, response.maxNapsPerDay());
        assertEquals("LOW", response.caffeineSensitivity());
        assertNull(response.caffeineDailyMg());
    }
}
