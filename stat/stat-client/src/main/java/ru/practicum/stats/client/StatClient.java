package ru.practicum.stats.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.client.exception.StatsServerUnavailable;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Component
public class StatClient {

    private static final Logger log = LoggerFactory.getLogger(StatClient.class);
    private static final DateTimeFormatter STATS_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final String statsServiceId;
    private final RetryTemplate retryTemplate;

    @Autowired
    public StatClient(RestClient restClient,
                      DiscoveryClient discoveryClient,
                      @Value("${stats.service.id}") String serviceId,
                      @Value("${stats.retry.max-attempts}") int maxAttempts,
                      @Value("${stats.retry.backoff-ms}") long backoffMs) {
        this.discoveryClient = discoveryClient;
        this.statsServiceId = serviceId;
        this.restClient = restClient;
        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .fixedBackoff(backoffMs)
                .retryOn(StatsServerUnavailable.class)
                .retryOn(ResourceAccessException.class)
                .build();
    }

    public void hit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();

        try {
            retryTemplate.execute(cxt -> {

                URI hitUri = buildUri("/hit", customizer -> {});

                restClient.post()
                        .uri(hitUri)
                        .body(dto)
                        .retrieve()
                        .toBodilessEntity();
                return null;
            });

            log.debug("Hit successfully sent to stats-service: app={}, uri={}, ip={}, timestamp={}",
                    app, uri, ip, timestamp);
        } catch (Exception e) {
            log.error("Failed to send hit: {}", e.getMessage(), e);
        }
    }

    public List<ViewStatsDto> getStat(LocalDateTime start,
                                      LocalDateTime end,
                                      List<String> uris,
                                      Boolean unique) {
        try {
            List<ViewStatsDto> stats = retryTemplate.execute(cxt -> {

                URI uri = buildUri("/stats", customizer -> {
                    customizer.queryParam("start", start.format(STATS_DATE_FORMAT))
                            .queryParam("end", end.format(STATS_DATE_FORMAT))
                            .queryParam("unique", unique);

                    if (uris != null && !uris.isEmpty()) {
                        customizer.queryParam("uris", uris);
                    }
                });

                return restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<ViewStatsDto>>() {
                        });
            });

            log.debug("Successfully requesting parameters to stats-service: start={}, end={}, uris={}, unique={} and received stats. Count={}",
                    start, end, uris, unique,
                    stats != null ? stats.size() : 0);

            return stats;
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

    private URI buildUri(String path, Consumer<UriComponentsBuilder> customizer) {
        ServiceInstance instance = getInstance();

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(instance.getHost())
                .port(instance.getPort())
                .path(path);
        customizer.accept(builder);

        return builder.build().encode().toUri();
    }
}
