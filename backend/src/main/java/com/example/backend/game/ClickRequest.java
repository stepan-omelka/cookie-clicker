package com.example.backend.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ClickRequest(@Min(1) @Max(GameService.MAX_CLICKS_PER_REQUEST) int count) {
}
