package com.midtone.backend.chat.application;

import com.midtone.backend.chat.domain.ChatFeedback;
import com.midtone.backend.chat.domain.ChatMessage;
import com.midtone.backend.chat.domain.ChatMessageRepository;
import com.midtone.backend.chat.domain.ChatResponseType;
import com.midtone.backend.chat.domain.ChatRole;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.global.time.DateTimeDefaults;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ChatMessageRepository repository; private final CurrentUserIdProvider userProvider;
    private final SafetyClassifier safetyClassifier; private final ChatAnswerGenerator answerGenerator;
    private final ChatSnapshotBuilder snapshotBuilder; private final ChatReferenceCatalog referenceCatalog;

    public ChatService(ChatMessageRepository repository, CurrentUserIdProvider userProvider,
            SafetyClassifier safetyClassifier, ChatAnswerGenerator answerGenerator,
            ChatSnapshotBuilder snapshotBuilder, ChatReferenceCatalog referenceCatalog) {
        this.repository = repository; this.userProvider = userProvider; this.safetyClassifier = safetyClassifier;
        this.answerGenerator = answerGenerator; this.snapshotBuilder = snapshotBuilder; this.referenceCatalog = referenceCatalog;
    }

    @Transactional
    public ChatSendResponse send(SendChatMessageRequest request, LocalDate today) {
        long userId = userProvider.getCurrentUserId();
        ChatMessage userMessage = repository.save(new ChatMessage(userId, ChatRole.USER, request.content(), null));
        SafetyClassifier.SafetyCategory safety = safetyClassifier.classify(request.content());
        if (safety == SafetyClassifier.SafetyCategory.EMERGENCY) return safetyResponse(userId, userMessage, true);
        if (safety == SafetyClassifier.SafetyCategory.MEDICAL) return safetyResponse(userId, userMessage, false);
        ChatContextSnapshot snapshot = snapshotBuilder.build(userId, today);
        ChatReference reference = referenceCatalog.match(request.content());
        GeneratedChatAnswer generated = answerGenerator.generate(
                new ChatPrompt(request.content(), snapshot, reference.excerpt(), reference.domain()));
        if (generated.safetyFlag() == SafetyFlag.EMERGENCY) return safetyResponse(userId, userMessage, true);
        if (generated.safetyFlag() == SafetyFlag.MEDICAL_REFERRAL) return safetyResponse(userId, userMessage, false);
        ChatResponseType type = ChatResponseType.AI;
        ChatMessage assistant = repository.save(new ChatMessage(userId, ChatRole.ASSISTANT, generated.answerText(), type));
        return new ChatSendResponse(userMessage.getId(), assistant.getId(), generated.answerText(), null, List.of(), null,
                generated.safetyFlag().name(), generated.citedDomain() == null ? null : generated.citedDomain().name(),
                snapshot,
                "일반적인 참고 정보이며 의학적 조언이 아닙니다.", List.of());
    }

    @Transactional
    public ChatFeedbackResponse feedback(long messageId, ChatFeedbackRequest request) {
        long userId = userProvider.getCurrentUserId();
        ChatMessage message = repository.findByIdAndUserId(messageId, userId)
                .orElseThrow(() -> new ChatException(ChatException.ErrorCode.NOT_FOUND));
        if (message.getRole() != ChatRole.ASSISTANT) throw new ChatException(ChatException.ErrorCode.ASSISTANT_ONLY);
        ChatFeedback feedback;
        try { feedback = ChatFeedback.valueOf(request.feedback()); }
        catch (IllegalArgumentException exception) { throw new ChatException(ChatException.ErrorCode.INVALID_FEEDBACK); }
        message.applyFeedback(feedback);
        return new ChatFeedbackResponse(messageId, feedback.name());
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse history(String cursor, int size) {
        if (size < 1 || size > 50) throw new ChatException(ChatException.ErrorCode.INVALID_SIZE);
        long userId = userProvider.getCurrentUserId();
        PageRequest pageable = PageRequest.of(0, size + 1);
        Page<ChatMessage> page = cursor == null || cursor.isBlank()
                ? repository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
                : repository.findByUserIdAndIdLessThanOrderByCreatedAtDescIdDesc(userId, decodeCursor(cursor), pageable);
        List<ChatMessage> fetched = new ArrayList<>(page.getContent());
        boolean hasNext = fetched.size() > size;
        if (hasNext) fetched.remove(fetched.size() - 1);
        String nextCursor = hasNext && !fetched.isEmpty() ? encodeCursor(fetched.get(fetched.size() - 1).getId()) : null;
        Collections.reverse(fetched);
        return new ChatHistoryResponse(fetched.stream().map(this::historyItem).toList(), nextCursor, hasNext);
    }

    private ChatHistoryResponse.Message historyItem(ChatMessage message) {
        String safetyFlag = switch (message.getResponseType() == null ? ChatResponseType.AI : message.getResponseType()) {
            case AI -> "NONE"; case SAFETY_MEDICAL -> "MEDICAL_REFERRAL"; case SAFETY_EMERGENCY -> "EMERGENCY";
        };
        return new ChatHistoryResponse.Message(message.getId(), message.getRole().name(), message.getContent(), safetyFlag,
                message.getFeedback() == null ? null : message.getFeedback().name(),
                message.getCreatedAt().atZone(DateTimeDefaults.DEFAULT_ZONE).toOffsetDateTime());
    }

    private long decodeCursor(String cursor) {
        try {
            return Long.parseLong(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            throw new ChatException(ChatException.ErrorCode.INVALID_CURSOR);
        }
    }

    private String encodeCursor(long id) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Long.toString(id).getBytes(StandardCharsets.UTF_8));
    }

    private ChatSendResponse safetyResponse(long userId, ChatMessage userMessage, boolean emergency) {
        String answer = emergency
                ? "즉시 119에 연락하거나 가까운 응급실의 도움을 받아 주세요. 혼자 있지 말고 주변 사람에게도 알려 주세요."
                : "복용 중인 약이나 질환에 대한 판단은 도와드리기 어렵습니다. 의사나 약사와 상담해 주세요.";
        ChatResponseType type = emergency ? ChatResponseType.SAFETY_EMERGENCY : ChatResponseType.SAFETY_MEDICAL;
        ChatMessage assistant = repository.save(new ChatMessage(userId, ChatRole.ASSISTANT, answer, type));
        var contacts = emergency ? List.of(new ChatSendResponse.EmergencyContact("119", "119"),
                new ChatSendResponse.EmergencyContact("자살예방상담전화", "109")) : List.<ChatSendResponse.EmergencyContact>of();
        return new ChatSendResponse(userMessage.getId(), assistant.getId(), answer, null, List.of(), null,
                emergency ? "EMERGENCY" : "MEDICAL_REFERRAL", null, null,
                emergency ? null : "약물 복용·용량·질환 진단에 대한 판단은 제공하지 않습니다.", contacts);
    }
}
