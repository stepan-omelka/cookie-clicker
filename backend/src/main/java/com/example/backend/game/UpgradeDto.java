package com.example.backend.game;

public record UpgradeDto(String id, String name, String description, long cost, int owned, int perClick, int perSecond) {
}
