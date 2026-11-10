package com.example.backend.game;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameStateEntityTest {

    private static final Instant START = Instant.parse("2026-11-10T10:00:00Z");

    @Test
    void earnsPassiveIncomeOverTime() {
        var state = new GameStateEntity(1L, 10, 0, START);

        assertThat(state.cookiesAt(START.plusMillis(2500), 4)).isEqualTo(20.0);
    }

    @Test
    void ignoresClockGoingBackwards() {
        var state = new GameStateEntity(1L, 10, 0, START);

        state.catchUp(START.minusSeconds(5), 4);

        assertThat(state.getCookies()).isEqualTo(10.0);
        assertThat(state.getUpdatedAt()).isEqualTo(START);
    }

    @Test
    void clicksUseCurrentPerClickValue() {
        var state = new GameStateEntity(1L, 0, 0, START);

        int accepted = state.click(3, 5);

        assertThat(accepted).isEqualTo(3);
        assertThat(state.getCookies()).isEqualTo(15.0);
        assertThat(state.getTotalClicks()).isEqualTo(3);
    }

    @Test
    void limitsHowFastClicksCanBeAdded() {
        var state = new GameStateEntity(1L, 0, 0, START);

        assertThat(state.click(500, 1)).isEqualTo(500);
        assertThat(state.click(500, 1)).isEqualTo(100);
        assertThat(state.click(500, 1)).isZero();

        state.catchUp(START.plusSeconds(2), 0);
        assertThat(state.click(500, 1)).isEqualTo(40);
        assertThat(state.getTotalClicks()).isEqualTo(640);
    }

    @Test
    void cannotSpendMoreThanAvailable() {
        var state = new GameStateEntity(1L, 14, 0, START);

        assertThatThrownBy(() -> state.spend(15)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upgradeCostGrowsWithEachPurchase() {
        var cursor = new UpgradeEntity("cursor", "Cursor", "", 15, 1, 0, 1);

        assertThat(cursor.costFor(0)).isEqualTo(15);
        assertThat(cursor.costFor(1)).isEqualTo(18);
        assertThat(cursor.costFor(10)).isEqualTo(61);
    }
}
