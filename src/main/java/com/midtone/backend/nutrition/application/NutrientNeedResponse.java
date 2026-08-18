package com.midtone.backend.nutrition.application;

import java.time.LocalDate;
import java.util.List;

public record NutrientNeedResponse(List<Item> needs) {
    public record Item(String nutrientCode, String source, LocalDate recordedOn) {}
}
