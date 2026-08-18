package com.midtone.backend.chat.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendChatMessageRequest(
        @NotBlank(message = "질문을 입력해 주세요.")
        @Size(max = 500, message = "질문은 500자를 초과할 수 없습니다.")
        String content) {
}
