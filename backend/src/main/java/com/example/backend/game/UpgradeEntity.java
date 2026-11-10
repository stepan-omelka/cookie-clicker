package com.example.backend.game;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "upgrade")
public class UpgradeEntity {

    @Id
    private String id;
    private String name;
    private String description;
    private long baseCost;
    private int perClick;
    private int perSecond;
    private int sortOrder;

    protected UpgradeEntity() {
    }

    public UpgradeEntity(String id, String name, String description, long baseCost, int perClick, int perSecond, int sortOrder) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseCost = baseCost;
        this.perClick = perClick;
        this.perSecond = perSecond;
        this.sortOrder = sortOrder;
    }

    public long costFor(int owned) {
        return (long) Math.ceil(baseCost * Math.pow(1.15, owned));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPerClick() {
        return perClick;
    }

    public int getPerSecond() {
        return perSecond;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
