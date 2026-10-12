package ru.practicum.stats.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Configuration
public class StatClientConfig {
    private static final Logger log = LoggerFactory.getLogger(StatClientConfig.class);

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                    log.error("Client error: {} - {}", response.getStatusCode(),
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
                })
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (request, response) -> {
                    log.error("Server error: {} - {}", response.getStatusCode(),
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
                })
                .build();
    }
}
