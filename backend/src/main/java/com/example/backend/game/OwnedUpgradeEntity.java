package com.example.backend.game;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "owned_upgrade")
public class OwnedUpgradeEntity {

    @Id
    private String upgradeId;
    private int quantity;

    protected OwnedUpgradeEntity() {
    }

    public OwnedUpgradeEntity(String upgradeId) {
        this.upgradeId = upgradeId;
        this.quantity = 0;
    }

    public void increment() {
        quantity++;
    }

    public String getUpgradeId() {
        return upgradeId;
    }

    public int getQuantity() {
        return quantity;
    }
}
