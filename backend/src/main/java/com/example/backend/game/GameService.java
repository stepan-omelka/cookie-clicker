package com.example.backend.game;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GameService {

    public static final int MAX_CLICKS_PER_REQUEST = 500;
    private static final long GAME_ID = 1L;
    private static final int BASE_PER_CLICK = 1;
    private static final Duration BATCH_RETENTION = Duration.ofHours(1);

    private final GameStateRepository gameStateRepository;
    private final UpgradeRepository upgradeRepository;
    private final OwnedUpgradeRepository ownedUpgradeRepository;
    private final ClickBatchRepository clickBatchRepository;
    private final Clock clock;

    public GameService(GameStateRepository gameStateRepository,
                       UpgradeRepository upgradeRepository,
                       OwnedUpgradeRepository ownedUpgradeRepository,
                       ClickBatchRepository clickBatchRepository,
                       Clock clock) {
        this.gameStateRepository = gameStateRepository;
        this.upgradeRepository = upgradeRepository;
        this.ownedUpgradeRepository = ownedUpgradeRepository;
        this.clickBatchRepository = clickBatchRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public GameDto getGame() {
        var state = gameStateRepository.findById(GAME_ID).orElseThrow(GameService::missingState);
        var production = loadProduction();
        var now = clock.instant();
        return toDto(state.cookiesAt(now, production.perSecond()), state, production);
    }

    @Transactional
    public GameDto click(UUID batchId, int count) {
        var state = lockState();
        var production = loadProduction();
        var now = clock.instant();
        state.catchUp(now, production.perSecond());
        // a retried batch is ignored, so a lost response never counts clicks twice
        if (clickBatchRepository.insertIfAbsent(batchId, now) == 1) {
            state.click(count, production.perClick());
        }
        clickBatchRepository.deleteOlderThan(now.minus(BATCH_RETENTION));
        return toDto(state.getCookies(), state, production);
    }

    @Transactional
    public GameDto buy(String upgradeId) {
        var upgrade = upgradeRepository.findById(upgradeId)
                .orElseThrow(() -> new UpgradeNotFoundException(upgradeId));
        var state = lockState();
        var production = loadProduction();
        state.catchUp(clock.instant(), production.perSecond());

        var owned = ownedUpgradeRepository.findById(upgradeId)
                .orElseGet(() -> new OwnedUpgradeEntity(upgradeId));
        long cost = upgrade.costFor(owned.getQuantity());
        if (!state.canAfford(cost)) {
            throw new NotEnoughCookiesException(cost, state.getCookies());
        }
        state.spend(cost);
        owned.increment();
        ownedUpgradeRepository.save(owned);

        return toDto(state.getCookies(), state, loadProduction());
    }

    @Transactional
    public GameDto reset() {
        var state = lockState();
        ownedUpgradeRepository.deleteAllInBatch();
        clickBatchRepository.deleteAllInBatch();
        state.reset(clock.instant());
        return toDto(state.getCookies(), state, loadProduction());
    }

    private GameStateEntity lockState() {
        return gameStateRepository.findByIdForUpdate(GAME_ID).orElseThrow(GameService::missingState);
    }

    private Production loadProduction() {
        var upgrades = upgradeRepository.findAllByOrderBySortOrderAsc();
        Map<String, Integer> owned = ownedUpgradeRepository.findAll().stream()
                .collect(Collectors.toMap(OwnedUpgradeEntity::getUpgradeId, OwnedUpgradeEntity::getQuantity));
        return Production.of(upgrades, owned);
    }

    private static GameDto toDto(double cookies, GameStateEntity state, Production production) {
        var upgrades = production.upgrades().stream()
                .map(upgrade -> {
                    int owned = production.owned(upgrade.getId());
                    return new UpgradeDto(upgrade.getId(), upgrade.getName(), upgrade.getDescription(),
                            upgrade.costFor(owned), owned, upgrade.getPerClick(), upgrade.getPerSecond());
                })
                .toList();
        return new GameDto(cookies, production.perClick(), production.perSecond(), state.getTotalClicks(), upgrades);
    }

    private static IllegalStateException missingState() {
        return new IllegalStateException("Game state row with id " + GAME_ID + " is missing in the database");
    }

    private record Production(List<UpgradeEntity> upgrades, Map<String, Integer> ownedById, long perClick, long perSecond) {

        static Production of(List<UpgradeEntity> upgrades, Map<String, Integer> ownedById) {
            long perClick = BASE_PER_CLICK;
            long perSecond = 0;
            for (var upgrade : upgrades) {
                int owned = ownedById.getOrDefault(upgrade.getId(), 0);
                perClick += (long) owned * upgrade.getPerClick();
                perSecond += (long) owned * upgrade.getPerSecond();
            }
            return new Production(upgrades, ownedById, perClick, perSecond);
        }

        int owned(String upgradeId) {
            return ownedById.getOrDefault(upgradeId, 0);
        }
    }
}
