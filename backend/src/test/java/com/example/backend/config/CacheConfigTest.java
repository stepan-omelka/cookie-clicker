package com.example.backend.config;

import com.example.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CacheConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageIsServedByIndexHtml() throws Exception {
        mockMvc.perform(get("/")).andExpect(forwardedUrl("index.html"));
    }

    @Test
    void indexHtmlIsFetchedAgainOnEveryVisit() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(header().doesNotExist("Last-Modified"));
    }

    @Test
    void apiResponsesAreNotStored() throws Exception {
        mockMvc.perform(get("/api/title"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
