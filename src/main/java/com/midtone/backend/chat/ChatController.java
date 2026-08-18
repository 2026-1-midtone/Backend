package com.midtone.backend.chat;

import com.midtone.backend.chat.application.ChatFeedbackRequest;
import com.midtone.backend.chat.application.ChatFeedbackResponse;
import com.midtone.backend.chat.application.ChatHistoryResponse;
import com.midtone.backend.chat.application.ChatSendResponse;
import com.midtone.backend.chat.application.ChatService;
import com.midtone.backend.chat.application.SendChatMessageRequest;
import com.midtone.backend.global.time.DateTimeDefaults;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chatService; private final Clock clock;
    public ChatController(ChatService chatService, Clock clock) { this.chatService = chatService; this.clock = clock; }

    @PostMapping("/messages")
    public ResponseEntity<ChatSendResponse> send(@Valid @RequestBody SendChatMessageRequest request) {
        return ResponseEntity.ok(chatService.send(request, LocalDate.now(clock.withZone(DateTimeDefaults.DEFAULT_ZONE))));
    }

    @PostMapping("/messages:recommend-products")
    public ResponseEntity<ChatSendResponse> recommendProducts() {
        return ResponseEntity.ok(
                chatService.recommendProducts(LocalDate.now(clock.withZone(DateTimeDefaults.DEFAULT_ZONE))));
    }

    @GetMapping("/messages")
    public ResponseEntity<ChatHistoryResponse> history(
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.history(cursor, size));
    }

    @PostMapping("/messages/{messageId}/feedback")
    public ResponseEntity<ChatFeedbackResponse> feedback(@PathVariable long messageId,
            @Valid @RequestBody ChatFeedbackRequest request) {
        return ResponseEntity.ok(chatService.feedback(messageId, request));
    }
}
