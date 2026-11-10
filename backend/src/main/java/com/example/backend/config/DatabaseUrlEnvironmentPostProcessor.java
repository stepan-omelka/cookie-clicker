package com.example.backend.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL_UNPOOLED", environment.getProperty("DATABASE_URL", ""));
        if (!databaseUrl.isBlank()) {
            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", toDataSourceProperties(databaseUrl)));
        }
    }

    // postgresql://user:password@host/db?sslmode=require -> spring.datasource.url, username, password
    static Map<String, Object> toDataSourceProperties(String databaseUrl) {
        var uri = URI.create(databaseUrl);
        if (!"postgresql".equals(uri.getScheme()) && !"postgres".equals(uri.getScheme())) {
            throw new IllegalArgumentException("DATABASE_URL must start with postgresql://");
        }
        String userInfo = uri.getRawUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalArgumentException("DATABASE_URL must contain a user and a password");
        }
        int separator = userInfo.indexOf(':');
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        return Map.of(
                "spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath() + jdbcQuery(uri.getRawQuery()),
                "spring.datasource.username", decode(userInfo.substring(0, separator)),
                "spring.datasource.password", decode(userInfo.substring(separator + 1)));
    }

    // Neon uses the libpq name, pgjdbc ignores it unless renamed
    private static String jdbcQuery(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        return "?" + Arrays.stream(rawQuery.split("&"))
                .map(parameter -> parameter.startsWith("channel_binding=")
                        ? "channelBinding=" + parameter.substring("channel_binding=".length())
                        : parameter)
                .collect(Collectors.joining("&"));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
