package com.midtone.backend.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.chat.application.ChatContextSnapshot;
import com.midtone.backend.chat.application.ChatSendResponse;
import com.midtone.backend.chat.application.ChatService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ChatControllerTest.FixedClockConfig.class)
class ChatControllerTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-08-19T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void 제품_추천_전용_엔드포인트가_추천_응답을_반환한다() throws Exception {
        LocalDate today = LocalDate.of(2026, 8, 19);
        given(chatService.recommendProducts(today)).willReturn(new ChatSendResponse(
                1L, 2L, "딥 슬립 앤 비전이 마그네슘 목표와 매칭됩니다.", null, List.of(), null,
                "NONE", "NUTRITION", ChatContextSnapshot.empty(today),
                "일반적인 참고 정보이며 의학적 조언이 아닙니다.", List.of()));

        mockMvc.perform(post("/api/v1/chat/messages:recommend-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("딥 슬립 앤 비전이 마그네슘 목표와 매칭됩니다."))
                .andExpect(jsonPath("$.citedDomain").value("NUTRITION"));
    }
}
