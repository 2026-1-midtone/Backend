package com.midtone.backend.chat.application;

import java.time.OffsetDateTime;
import java.util.List;

public record ChatHistoryResponse(List<Message> messages, String nextCursor, boolean hasNext) {
    public record Message(long messageId, String role, String content, String safetyFlag,
                          String feedback, OffsetDateTime createdAt) {}
}
