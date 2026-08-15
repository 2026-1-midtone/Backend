package com.midtone.backend.transition.application;

import java.util.List;

public record TransitionListResponse(List<Item> transitions) {

    public record Item(String transitionDate, String fromShiftType, String toShiftType, String protocolName) {
    }
}
