package com.example.backend.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsNeonConnectionString() {
        var properties = DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties(
                "postgresql://neondb_owner:npg_s3cr%40t@ep-cool-name-123.eu-central-1.aws.neon.tech/neondb?channel_binding=require&sslmode=require");

        assertThat(properties)
                .containsEntry("spring.datasource.url",
                        "jdbc:postgresql://ep-cool-name-123.eu-central-1.aws.neon.tech/neondb?channelBinding=require&sslmode=require")
                .containsEntry("spring.datasource.username", "neondb_owner")
                .containsEntry("spring.datasource.password", "npg_s3cr@t");
    }

    @Test
    void keepsPlusSignInPassword() {
        var properties = DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties("postgresql://app:a+b@localhost/game");

        assertThat(properties).containsEntry("spring.datasource.password", "a+b");
    }

    @Test
    void keepsExplicitPort() {
        var properties = DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties("postgres://app:pw@localhost:5433/game");

        assertThat(properties).containsEntry("spring.datasource.url", "jdbc:postgresql://localhost:5433/game");
    }

    @Test
    void rejectsUrlWithoutCredentials() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties("postgresql://host/db"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
