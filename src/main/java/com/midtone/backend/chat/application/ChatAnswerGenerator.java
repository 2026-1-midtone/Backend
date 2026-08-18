package com.midtone.backend.chat.application;

public interface ChatAnswerGenerator {
    GeneratedChatAnswer generate(ChatPrompt prompt);
}
