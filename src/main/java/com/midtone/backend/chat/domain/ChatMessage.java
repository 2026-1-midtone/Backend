package com.midtone.backend.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_type", length = 30)
    private ChatResponseType responseType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ChatFeedback feedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(Long userId, ChatRole role, String content, ChatResponseType responseType) {
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.responseType = responseType;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public ChatRole getRole() { return role; }
    public String getContent() { return content; }
    public ChatResponseType getResponseType() { return responseType; }
    public ChatFeedback getFeedback() { return feedback; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void applyFeedback(ChatFeedback feedback) {
        this.feedback = feedback;
    }
}
