package com.example.backend.game;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public GameDto getGame() {
        return gameService.getGame();
    }

    @PostMapping("/clicks")
    public GameDto click(@Valid @RequestBody ClickRequest request) {
        return gameService.click(request.id(), request.count());
    }

    @PostMapping("/upgrades/{upgradeId}")
    public GameDto buy(@PathVariable String upgradeId) {
        return gameService.buy(upgradeId);
    }

    @DeleteMapping
    public GameDto reset() {
        return gameService.reset();
    }
}
