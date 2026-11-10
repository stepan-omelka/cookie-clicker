package com.example.backend.game;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "game_state")
public class GameStateEntity {

    static final double CLICKS_PER_SECOND_LIMIT = 20;
    static final double MAX_CLICK_ALLOWANCE = 600;

    @Id
    private Long id;
    private double cookies;
    private long totalClicks;
    private double clickAllowance;
    private Instant updatedAt;

    protected GameStateEntity() {
    }

    public GameStateEntity(Long id, double cookies, long totalClicks, Instant updatedAt) {
        this.id = id;
        this.cookies = cookies;
        this.totalClicks = totalClicks;
        this.clickAllowance = MAX_CLICK_ALLOWANCE;
        this.updatedAt = updatedAt;
    }

    public double cookiesAt(Instant now, long perSecond) {
        return cookies + perSecond * secondsSinceUpdate(now);
    }

    public void catchUp(Instant now, long perSecond) {
        double elapsed = secondsSinceUpdate(now);
        cookies += perSecond * elapsed;
        clickAllowance = Math.min(MAX_CLICK_ALLOWANCE, clickAllowance + elapsed * CLICKS_PER_SECOND_LIMIT);
        if (now.isAfter(updatedAt)) {
            updatedAt = now;
        }
    }

    public int click(int count, long perClick) {
        int accepted = (int) Math.min(count, Math.floor(clickAllowance));
        clickAllowance -= accepted;
        cookies += (double) accepted * perClick;
        totalClicks += accepted;
        return accepted;
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
        clickAllowance = MAX_CLICK_ALLOWANCE;
        updatedAt = now;
    }

    private double secondsSinceUpdate(Instant now) {
        return Math.max(0, Duration.between(updatedAt, now).toNanos()) / 1e9;
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
