package com.midtone.backend.chat.application;

import java.util.List;

public record ChatSendResponse(Long userMessageId, Long assistantMessageId, String answer,
        String verdict, List<String> reasons, List<String> alternatives, String safetyFlag,
        String citedDomain, ChatContextSnapshot context, String disclaimer,
        List<EmergencyContact> emergencyContacts) {
    public record EmergencyContact(String name, String number) {}
}
