package com.midtone.backend.chat.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

import com.midtone.backend.chat.domain.ChatMessage;
import com.midtone.backend.chat.domain.ChatMessageRepository;
import com.midtone.backend.chat.domain.ChatResponseType;
import com.midtone.backend.chat.domain.ChatRole;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatMessageRepository repository;
    @Mock CurrentUserIdProvider currentUserIdProvider;
    @Mock ChatAnswerGenerator answerGenerator;
    @Mock ChatSnapshotBuilder snapshotBuilder;
    @Mock ChatReferenceCatalog referenceCatalog;

    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(repository, currentUserIdProvider, new SafetyClassifier(),
                answerGenerator, snapshotBuilder, referenceCatalog);
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        lenient().when(repository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 응급_질문은_AI를_호출하지_않고_긴급_안내를_저장한다() {
        ChatSendResponse response = service.send(new SendChatMessageRequest("죽고 싶어"), LocalDate.parse("2026-08-18"));

        assertEquals("EMERGENCY", response.safetyFlag());
        assertEquals(2, response.emergencyContacts().size());
        verify(answerGenerator, never()).generate(any());
        verify(repository, org.mockito.Mockito.times(2)).save(any(ChatMessage.class));
    }

    @Test
    void 일반_질문은_계산된_스냅샷과_근거를_AI에_전달한다() {
        LocalDate today = LocalDate.parse("2026-08-18");
        ChatContextSnapshot snapshot = ChatContextSnapshot.empty(today);
        given(snapshotBuilder.build(1L, today)).willReturn(snapshot);
        given(referenceCatalog.match("지금 커피 마셔도 돼?")).willReturn(
                new ChatReference(ChatDomain.CAFFEINE, "카페인 근거"));
        given(answerGenerator.generate(any())).willReturn(
                new GeneratedChatAnswer("지금은 컨텍스트의 컷오프 시각을 확인해 주세요.", SafetyFlag.NONE, ChatDomain.CAFFEINE));

        ChatSendResponse response = service.send(new SendChatMessageRequest("지금 커피 마셔도 돼?"), today);

        assertEquals("NONE", response.safetyFlag());
        assertEquals("CAFFEINE", response.citedDomain());
        verify(answerGenerator).generate(org.mockito.ArgumentMatchers.argThat(prompt ->
                prompt.contextSnapshot() == snapshot && prompt.referenceExcerpt().equals("카페인 근거")));
    }

    @Test
    void 사용자_메시지에는_피드백할_수_없다() {
        given(repository.findByIdAndUserId(10L, 1L)).willReturn(java.util.Optional.of(
                new ChatMessage(1L, ChatRole.USER, "질문", null)));

        ChatException exception = assertThrows(ChatException.class,
                () -> service.feedback(10L, new ChatFeedbackRequest("LIKE")));

        assertEquals(ChatException.ErrorCode.ASSISTANT_ONLY, exception.getErrorCode());
    }

    @Test
    void 제품_추천_요청은_추천_전용_근거로_AI를_호출한다() {
        LocalDate today = LocalDate.parse("2026-08-19");
        ChatContextSnapshot snapshot = new ChatContextSnapshot(today, null, null, java.util.List.of(), null, null, null,
                java.util.List.of(), new ChatContextSnapshot.RoutineProgress(0, 0),
                new com.midtone.backend.nutrition.application.NutrientNeedResponse(java.util.List.of(
                        new com.midtone.backend.nutrition.application.NutrientNeedResponse.Item("MAGNESIUM", "USER", today))),
                new com.midtone.backend.nutrition.application.NutritionRecommendationResponse(java.util.List.of(
                        new com.midtone.backend.nutrition.application.NutritionRecommendationResponse.Recommendation(
                                1L, "DEEP_SLEEP_VISION", "바이브젠 딥 슬립 앤 비전", "VIVEGEN DEEP SLEEP & VISION",
                                "/images/products/deep_sleep_vision.png",
                                "https://www.vivegen.co.kr/shop_view?idx=321",
                                java.util.List.of("MAGNESIUM"), java.util.List.of(), "건강기능식품은 의약품이 아닙니다."))));
        given(snapshotBuilder.build(1L, today)).willReturn(snapshot);
        given(referenceCatalog.productRecommendation()).willReturn(
                new ChatReference(ChatDomain.NUTRITION, "추천 전용 근거"));
        given(answerGenerator.generate(any())).willReturn(
                new GeneratedChatAnswer("딥 슬립 앤 비전이 마그네슘 목표와 매칭됩니다.", SafetyFlag.NONE, ChatDomain.NUTRITION));

        ChatSendResponse response = service.recommendProducts(today);

        assertEquals("NONE", response.safetyFlag());
        assertEquals("NUTRITION", response.citedDomain());
        verify(answerGenerator).generate(org.mockito.ArgumentMatchers.argThat(prompt ->
                prompt.contextSnapshot() == snapshot
                        && prompt.referenceExcerpt().equals("추천 전용 근거")
                        && prompt.domain() == ChatDomain.NUTRITION));
        verify(repository, org.mockito.Mockito.times(2)).save(any(ChatMessage.class));
    }

    @Test
    void 영양소_목표가_없으면_AI_호출_없이_등록을_안내한다() {
        LocalDate today = LocalDate.parse("2026-08-19");
        given(snapshotBuilder.build(1L, today)).willReturn(ChatContextSnapshot.empty(today));

        ChatSendResponse response = service.recommendProducts(today);

        assertEquals("NONE", response.safetyFlag());
        org.junit.jupiter.api.Assertions.assertTrue(response.answer().contains("영양소 목표"));
        verify(answerGenerator, never()).generate(any());
        verify(repository, org.mockito.Mockito.times(2)).save(any(ChatMessage.class));
    }

    @Test
    void 목표는_있지만_매칭_제품이_없으면_AI_호출_없이_안내한다() {
        LocalDate today = LocalDate.parse("2026-08-19");
        ChatContextSnapshot snapshot = new ChatContextSnapshot(today, null, null, java.util.List.of(), null, null, null,
                java.util.List.of(), new ChatContextSnapshot.RoutineProgress(0, 0),
                new com.midtone.backend.nutrition.application.NutrientNeedResponse(java.util.List.of(
                        new com.midtone.backend.nutrition.application.NutrientNeedResponse.Item("MAGNESIUM", "USER", today))),
                new com.midtone.backend.nutrition.application.NutritionRecommendationResponse(java.util.List.of()));
        given(snapshotBuilder.build(1L, today)).willReturn(snapshot);

        ChatSendResponse response = service.recommendProducts(today);

        org.junit.jupiter.api.Assertions.assertTrue(response.answer().contains("매칭"));
        verify(answerGenerator, never()).generate(any());
    }
}
