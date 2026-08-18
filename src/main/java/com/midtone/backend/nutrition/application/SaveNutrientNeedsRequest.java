package com.midtone.backend.nutrition.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveNutrientNeedsRequest(
        @NotNull @Size(max = 15) List<@Valid Item> needs) {
    public record Item(@NotBlank String nutrientCode, String source, String recordedOn) {}
}
