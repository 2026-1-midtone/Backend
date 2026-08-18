package com.midtone.backend.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.chat.application.ChatAnswerGenerator;
import com.midtone.backend.chat.application.ChatDomain;
import com.midtone.backend.chat.application.GeneratedChatAnswer;
import com.midtone.backend.chat.application.SafetyFlag;
import com.midtone.backend.chat.domain.ChatMessageRepository;
import com.midtone.backend.nutrition.domain.NutrientCode;
import com.midtone.backend.nutrition.domain.NutrientNeedSource;
import com.midtone.backend.nutrition.domain.UserNutrientNeed;
import com.midtone.backend.nutrition.domain.UserNutrientNeedRepository;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import com.midtone.backend.user.domain.User;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class ChatRecommendationIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserNutrientNeedRepository needRepository;

    @Autowired
    private TestUserFixture testUserFixture;

    @MockitoBean
    private ChatAnswerGenerator chatAnswerGenerator;

    private User user;
    private String authorization;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        needRepository.deleteAll();
        user = testUserFixture.createUserWithSettings("chat-reco-" + System.nanoTime());
        authorization = "Bearer " + jwtProvider.createAccessToken(user.getId());
    }

    @Test
    void 영양소_목표가_있으면_시드_제품_후보를_담아_AI를_호출한다() throws Exception {
        needRepository.save(new UserNutrientNeed(
                user.getId(), NutrientCode.MAGNESIUM, NutrientNeedSource.USER_REPORTED, LocalDate.parse("2026-08-19")));
        given(chatAnswerGenerator.generate(any())).willReturn(new GeneratedChatAnswer(
                "마그네슘 목표와 매칭되는 제품을 소개해 드릴게요.", SafetyFlag.NONE, ChatDomain.NUTRITION));

        mockMvc.perform(post("/api/v1/chat/messages:recommend-products").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citedDomain").value("NUTRITION"))
                .andExpect(jsonPath("$.answer").value("마그네슘 목표와 매칭되는 제품을 소개해 드릴게요."))
                .andExpect(jsonPath("$.context.nutritionRecommendations.recommendations",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));

        verify(chatAnswerGenerator).generate(org.mockito.ArgumentMatchers.argThat(prompt ->
                prompt.domain() == ChatDomain.NUTRITION
                        && !prompt.contextSnapshot().nutritionRecommendations().recommendations().isEmpty()
                        && prompt.contextSnapshot().nutritionRecommendations().recommendations().stream()
                                .allMatch(item -> item.matchedNutrients().contains("MAGNESIUM"))));
        assertEquals(2, chatMessageRepository.count());
    }

    @Test
    void 영양소_목표가_없으면_AI_호출_없이_등록_안내를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/chat/messages:recommend-products").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", org.hamcrest.Matchers.containsString("영양소 목표")));

        verify(chatAnswerGenerator, never()).generate(any());
        assertEquals(2, chatMessageRepository.count());
    }
}
