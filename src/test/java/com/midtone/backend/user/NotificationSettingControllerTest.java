package com.midtone.backend.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.user.application.notification.NotificationSettingResponse;
import com.midtone.backend.user.application.notification.NotificationSettingSaveResponse;
import com.midtone.backend.user.application.notification.NotificationSettingService;
import com.midtone.backend.user.application.notification.NotificationSettingsResponse;
import com.midtone.backend.user.application.notification.SaveNotificationSettingsRequest;
import com.midtone.backend.user.application.notification.SaveNotificationSettingsResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationSettingController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationSettingService notificationSettingService;

    @Test
    void returnsCurrentNotificationSettings() throws Exception {
        given(notificationSettingService.getSettings()).willReturn(new NotificationSettingsResponse(
                List.of(new NotificationSettingResponse("NAP", true, null, "14:00"))));

        mockMvc.perform(get("/api/v1/users/me/notification-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings[0].type").value("NAP"))
                .andExpect(jsonPath("$.settings[0].enabled").value(true));
    }

    @Test
    void savesNotificationSettings() throws Exception {
        given(notificationSettingService.saveSettings(any(SaveNotificationSettingsRequest.class)))
                .willReturn(new SaveNotificationSettingsResponse(
                        List.of(new NotificationSettingSaveResponse("NAP", false, null))));

        mockMvc.perform(put("/api/v1/users/me/notification-settings")
                        .contentType("application/json")
                        .content("{\"settings\":[{\"type\":\"NAP\",\"enabled\":false,\"customTime\":null}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings[0].enabled").value(false));
    }

    @Test
    void rejectsSaveWithEmptySettings() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/notification-settings")
                        .contentType("application/json")
                        .content("{\"settings\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("settings는 필수 입력값입니다."));
    }

    @Test
    void rejectsSaveWithoutNotificationType() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/notification-settings")
                        .contentType("application/json")
                        .content("{\"settings\":[{\"enabled\":false,\"customTime\":null}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("알림 유형은 필수 입력값입니다."));
    }
}
