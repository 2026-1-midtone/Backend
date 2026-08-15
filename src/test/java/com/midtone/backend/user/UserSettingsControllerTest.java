package com.midtone.backend.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.user.application.settings.SaveSettingsRequest;
import com.midtone.backend.user.application.settings.SettingsResponse;
import com.midtone.backend.user.application.settings.UserSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserSettingsService userSettingsService;

    @Test
    void returnsCurrentSettings() throws Exception {
        given(userSettingsService.getSettings()).willReturn(new SettingsResponse(300, "MEDIUM", 20, 2));

        mockMvc.perform(get("/api/v1/users/me/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caffeineDailyMg").value(300))
                .andExpect(jsonPath("$.preferredNapMinutes").value(20));
    }

    @Test
    void savesSettings() throws Exception {
        given(userSettingsService.saveSettings(any(SaveSettingsRequest.class)))
                .willReturn(new SettingsResponse(400, "HIGH", 30, 3));

        mockMvc.perform(put("/api/v1/users/me/settings")
                        .contentType("application/json")
                        .content("{\"caffeineDailyMg\":400,\"caffeineSensitivity\":\"HIGH\",\"preferredNapMinutes\":30,\"maxNapsPerDay\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caffeineSensitivity").value("HIGH"))
                .andExpect(jsonPath("$.maxNapsPerDay").value(3));
    }

    @Test
    void rejectsSaveWithoutPreferredNapMinutes() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/settings")
                        .contentType("application/json")
                        .content("{\"maxNapsPerDay\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("preferredNapMinutes은 필수 입력값입니다."));
    }
}
