package com.example.backend.game;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "game_state")
public class GameStateEntity {

    @Id
    private Long id;
    private double cookies;
    private long totalClicks;
    private Instant updatedAt;

    protected GameStateEntity() {
    }

    public GameStateEntity(Long id, double cookies, long totalClicks, Instant updatedAt) {
        this.id = id;
        this.cookies = cookies;
        this.totalClicks = totalClicks;
        this.updatedAt = updatedAt;
    }

    public double cookiesAt(Instant now, long perSecond) {
        long elapsedMillis = Math.max(0, Duration.between(updatedAt, now).toMillis());
        return cookies + perSecond * elapsedMillis / 1000.0;
    }

    public void collectPassiveIncome(Instant now, long perSecond) {
        cookies = cookiesAt(now, perSecond);
        if (now.isAfter(updatedAt)) {
            updatedAt = now;
        }
    }

    public void click(int count, long perClick) {
        cookies += (double) count * perClick;
        totalClicks += count;
    }

    public boolean canAfford(long cost) {
        return cookies >= cost;
    }

    public void spend(long cost) {
        if (!canAfford(cost)) {
            throw new IllegalArgumentException("Cannot spend more cookies than available");
        }
        cookies -= cost;
    }

    public void reset(Instant now) {
        cookies = 0;
        totalClicks = 0;
        updatedAt = now;
    }

    public double getCookies() {
        return cookies;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
