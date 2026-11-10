package com.example.backend.game;

public class UpgradeNotFoundException extends RuntimeException {

    public UpgradeNotFoundException(String upgradeId) {
        super("Upgrade '" + upgradeId + "' does not exist");
    }
}
