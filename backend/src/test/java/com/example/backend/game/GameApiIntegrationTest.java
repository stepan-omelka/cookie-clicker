package com.example.backend.game;

import com.example.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GameApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetGame() throws Exception {
        mockMvc.perform(delete("/api/game")).andExpect(status().isOk());
    }

    @Test
    void returnsTitleFromDatabase() throws Exception {
        mockMvc.perform(get("/api/title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Cookie Clicker"));
    }

    @Test
    void startsWithEmptyGameAndAllUpgrades() throws Exception {
        mockMvc.perform(get("/api/game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cookies").value(0.0))
                .andExpect(jsonPath("$.perClick").value(1))
                .andExpect(jsonPath("$.perSecond").value(0))
                .andExpect(jsonPath("$.upgrades.length()").value(5))
                .andExpect(jsonPath("$.upgrades[0].id").value("cursor"))
                .andExpect(jsonPath("$.upgrades[0].cost").value(15));
    }

    @Test
    void clicksAddCookies() throws Exception {
        click(20)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cookies").value(20.0))
                .andExpect(jsonPath("$.totalClicks").value(20));
    }

    @Test
    void retriedBatchIsCountedOnce() throws Exception {
        var batchId = UUID.randomUUID();

        click(batchId, 10).andExpect(jsonPath("$.totalClicks").value(10));
        click(batchId, 10).andExpect(status().isOk()).andExpect(jsonPath("$.totalClicks").value(10));
    }

    @Test
    void batchRetriedAfterResetIsNotCountedAgain() throws Exception {
        var batchId = UUID.randomUUID();
        click(batchId, 10);

        mockMvc.perform(delete("/api/game"));

        click(batchId, 10).andExpect(status().isOk()).andExpect(jsonPath("$.totalClicks").value(0));
    }

    @Test
    void parallelClicksAreNotLost() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            var tasks = new ArrayList<Callable<Integer>>();
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> click(5).andReturn().getResponse().getStatus());
            }
            for (var result : executor.invokeAll(tasks)) {
                assertThat(result.get()).isEqualTo(200);
            }
        } finally {
            executor.shutdown();
        }

        mockMvc.perform(get("/api/game")).andExpect(jsonPath("$.totalClicks").value(100));
    }

    @Test
    void buyingUpgradeSpendsCookiesAndRaisesPrice() throws Exception {
        click(20);

        mockMvc.perform(post("/api/game/upgrades/cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cookies").value(5.0))
                .andExpect(jsonPath("$.perClick").value(2))
                .andExpect(jsonPath("$.upgrades[0].owned").value(1))
                .andExpect(jsonPath("$.upgrades[0].cost").value(18));
    }

    @Test
    void rejectsPurchaseWithoutEnoughCookies() throws Exception {
        mockMvc.perform(post("/api/game/upgrades/grandma"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Upgrade costs 100 cookies, you have 0"));
    }

    @Test
    void rejectsUnknownUpgrade() throws Exception {
        mockMvc.perform(post("/api/game/upgrades/time-machine"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidClickCount() throws Exception {
        click(0).andExpect(status().isBadRequest());
        click(GameService.MAX_CLICKS_PER_REQUEST + 1).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/game/clicks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\": 5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetClearsProgress() throws Exception {
        click(20);
        mockMvc.perform(post("/api/game/upgrades/cursor"));

        mockMvc.perform(delete("/api/game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cookies").value(0.0))
                .andExpect(jsonPath("$.totalClicks").value(0))
                .andExpect(jsonPath("$.upgrades[0].owned").value(0));
    }

    private ResultActions click(int count) throws Exception {
        return click(UUID.randomUUID(), count);
    }

    private ResultActions click(UUID batchId, int count) throws Exception {
        return mockMvc.perform(post("/api/game/clicks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": \"" + batchId + "\", \"count\": " + count + "}"));
    }
}
