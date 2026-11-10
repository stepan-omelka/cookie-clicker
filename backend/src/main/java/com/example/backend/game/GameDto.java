package com.example.backend.game;

import java.util.List;

public record GameDto(double cookies, long perClick, long perSecond, long totalClicks, List<UpgradeDto> upgrades) {
}
