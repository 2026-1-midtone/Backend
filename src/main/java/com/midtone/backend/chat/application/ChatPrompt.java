package com.midtone.backend.chat.application;

public record ChatPrompt(String question, ChatContextSnapshot contextSnapshot,
                         String referenceExcerpt, ChatDomain domain) {
}
