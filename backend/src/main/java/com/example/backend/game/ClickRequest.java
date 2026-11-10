package com.example.backend.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClickRequest(@NotNull UUID id, @Min(1) @Max(GameService.MAX_CLICKS_PER_REQUEST) int count) {
}
