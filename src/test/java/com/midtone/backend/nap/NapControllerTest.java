package com.midtone.backend.nap;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.nap.application.NapService;
import com.midtone.backend.nap.application.NapService.ActiveNap;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NapController.class)
@AutoConfigureMockMvc(addFilters = false)
class NapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NapService napService;

    @Test
    void returnsActiveNapForLocalDevelopmentUser() throws Exception {
        given(napService.getActiveNap()).willReturn(new ActiveNap(
                44L,
                20,
                OffsetDateTime.parse("2026-08-07T18:00:00+09:00"),
                OffsetDateTime.parse("2026-08-07T18:20:00+09:00"),
                720,
                "RUNNING"));

        mockMvc.perform(get("/api/v1/naps/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.napId").value(44))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void returnsNullActiveNapWhenNoNapIsRunning() throws Exception {
        given(napService.getActiveNap()).willReturn(null);

        mockMvc.perform(get("/api/v1/naps/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeNap").isEmpty());
    }
}
