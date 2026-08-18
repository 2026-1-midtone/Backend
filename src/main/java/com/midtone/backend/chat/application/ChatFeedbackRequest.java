package com.midtone.backend.chat.application;

import jakarta.validation.constraints.NotBlank;

public record ChatFeedbackRequest(@NotBlank String feedback) {
}
