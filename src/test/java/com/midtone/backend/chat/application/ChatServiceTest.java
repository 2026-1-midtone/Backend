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
}
