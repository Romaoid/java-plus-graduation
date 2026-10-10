package client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import ru.practicum.stats.client.StatClient;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatClientTest {

    private static final String STATS_SERVICE_ID = "stats-server";
    private static final String HOST = "stats-host";
    private static final int PORT = 9090;

    private StatClient statClient;

    @Mock
    private DiscoveryClient discoveryClient;
    @Mock
    private ServiceInstance serviceInstance;
    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpecForHit;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpecForGet;

    @Captor
    private ArgumentCaptor<EndpointHitDto> dtoCaptor;
    @Captor
    private ArgumentCaptor<ParameterizedTypeReference<List<ViewStatsDto>>> typeReferenceCaptor;
    @Captor
    private ArgumentCaptor<URI> uriCaptor;

    @BeforeEach
    void setUp() {
        statClient = new StatClient(restClient, discoveryClient, STATS_SERVICE_ID, 3, 1L);

        lenient().when(discoveryClient.getInstances(STATS_SERVICE_ID))
                .thenReturn(List.of(serviceInstance));
        lenient().when(serviceInstance.getHost()).thenReturn(HOST);
        lenient().when(serviceInstance.getPort()).thenReturn(PORT);
    }

    @Test
    void hit_shouldSendSuccessfulRequest() {
        String app = "test-app";
        String uri = "/api/test";
        String ip = "127.0.0.1";
        LocalDateTime timestamp = LocalDateTime.now();

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(EndpointHitDto.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpecForHit);
        when(responseSpecForHit.toBodilessEntity()).thenReturn(null);

        statClient.hit(app, uri, ip, timestamp);

        verify(restClient).post();
        verify(requestBodyUriSpec).uri(uriCaptor.capture());
        verify(requestBodySpec).body(dtoCaptor.capture());
        verify(requestBodySpec).retrieve();
        verify(responseSpecForHit).toBodilessEntity();

        EndpointHitDto capturedDto = dtoCaptor.getValue();
        assertThat(capturedDto.getApp()).isEqualTo(app);
        assertThat(capturedDto.getUri()).isEqualTo(uri);
        assertThat(capturedDto.getIp()).isEqualTo(ip);
        assertThat(capturedDto.getTimestamp()).isEqualTo(timestamp);
        assertThat(uriCaptor.getValue().toString()).isEqualTo("http://stats-host:9090/hit");
    }

    @Test
    void hit_shouldHandleDateTimeWithSeconds() {
        LocalDateTime timestamp = LocalDateTime.now();

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(EndpointHitDto.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpecForHit);
        when(responseSpecForHit.toBodilessEntity()).thenReturn(null);

        statClient.hit("app", "/uri", "ip", timestamp);

        verify(requestBodySpec).body(dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void hit_shouldRetryOnResourceAccessException() {
        when(restClient.post()).thenThrow(new ResourceAccessException("Connection refused"));

        statClient.hit("app", "/uri", "ip", LocalDateTime.now());

        verify(restClient, times(3)).post();
    }

    @Test
    void hit_shouldSwallowAnyException() {
        when(restClient.post()).thenThrow(new RuntimeException("some error"));

        assertThatNoException().isThrownBy(() ->
                statClient.hit("app", "/uri", "ip", LocalDateTime.now()));

        verify(restClient).post();
    }

    @Test
    void hit_shouldPreserveAllDtoFields() {
        String app = "event-service";
        String uri = "/events/123";
        String ip = "192.168.1.100";
        LocalDateTime timestamp = LocalDateTime.now();

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(EndpointHitDto.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpecForHit);
        when(responseSpecForHit.toBodilessEntity()).thenReturn(null);

        statClient.hit(app, uri, ip, timestamp);

        verify(requestBodySpec).body(dtoCaptor.capture());
        EndpointHitDto dto = dtoCaptor.getValue();

        assertThat(dto.getApp()).isEqualTo(app);
        assertThat(dto.getUri()).isEqualTo(uri);
        assertThat(dto.getIp()).isEqualTo(ip);
        assertThat(dto.getTimestamp()).isEqualTo(timestamp);
        assertThat(dto.getId()).isNull();
    }

    @Test
    void getStat_shouldReturnListWhenSuccess() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 2, 0, 0, 0);
        List<String> uris = List.of("/events", "/events/1");
        Boolean unique = true;

        List<ViewStatsDto> expectedStats = List.of(
                ViewStatsDto.builder().app("app1").uri("/events").hits(10L).build(),
                ViewStatsDto.builder().app("app2").uri("/events/1").hits(5L).build()
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpecForGet);
        when(responseSpecForGet.body(any(ParameterizedTypeReference.class))).thenReturn(expectedStats);

        List<ViewStatsDto> result = statClient.getStat(start, end, uris, unique);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getHits()).isEqualTo(10);
        assertThat(result.get(1).getHits()).isEqualTo(5);

        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(any(URI.class));
        verify(requestHeadersSpec).retrieve();
        verify(responseSpecForGet).body(any(ParameterizedTypeReference.class));
    }

    @Test
    void getStat_shouldBuildCorrectUrlWithSingleUri() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 1, 18, 0, 0);
        List<String> uris = List.of("/events");
        Boolean unique = false;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpecForGet);
        when(responseSpecForGet.body(any(ParameterizedTypeReference.class))).thenReturn(Collections.emptyList());

        statClient.getStat(start, end, uris, unique);

        verify(requestHeadersUriSpec).uri(uriCaptor.capture());

        UriComponents url = UriComponentsBuilder.fromUri(uriCaptor.getValue()).build();
        assertThat(url.getQueryParams().get("unique")).containsExactly("false");
        assertThat(url.getQueryParams().get("uris")).containsExactly("/events");

        assertThat(decodeUrl(url, "start"))
                .isEqualTo("2024-01-01 12:00:00");

        assertThat(decodeUrl(url, "end"))
                .isEqualTo("2024-01-01 18:00:00");
    }

    @Test
    void getStat_shouldBuildCorrectUrlWithMultipleUris() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 2, 0, 0, 0);
        List<String> uris = List.of("/events", "/events/1", "/events/2");
        Boolean unique = true;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpecForGet);
        when(responseSpecForGet.body(any(ParameterizedTypeReference.class))).thenReturn(Collections.emptyList());

        statClient.getStat(start, end, uris, unique);

        verify(requestHeadersUriSpec).uri(uriCaptor.capture());

        UriComponents url = UriComponentsBuilder.fromUri(uriCaptor.getValue()).build();
        assertThat(url.getQueryParams().get("uris"))
                .containsExactly("/events", "/events/1", "/events/2");
        assertThat(url.getQueryParams().get("unique")).containsExactly("true");
    }

    @Test
    void getStat_shouldHandleNullUris() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 2, 0, 0, 0);
        List<String> uris = null;
        Boolean unique = false;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpecForGet);
        when(responseSpecForGet.body(any(ParameterizedTypeReference.class))).thenReturn(Collections.emptyList());

        statClient.getStat(start, end, uris, unique);

        verify(requestHeadersUriSpec).uri(uriCaptor.capture());

        UriComponents url = UriComponentsBuilder.fromUri(uriCaptor.getValue()).build();
        assertThat(url.getQueryParams().get("unique")).containsExactly("false");
        assertThat(url.getQueryParams()).doesNotContainKey("uris");

        assertThat(decodeUrl(url, "start"))
                .isEqualTo("2024-01-01 00:00:00");

        assertThat(decodeUrl(url, "end"))
                .isEqualTo("2024-01-02 00:00:00");
    }

    @Test
    void getStat_shouldHandleEmptyUrisList() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 2, 0, 0, 0);
        List<String> uris = Collections.emptyList();
        Boolean unique = false;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpecForGet);
        when(responseSpecForGet.body(any(ParameterizedTypeReference.class))).thenReturn(Collections.emptyList());

        statClient.getStat(start, end, uris, unique);

        verify(requestHeadersUriSpec).uri(uriCaptor.capture());

        UriComponents url = UriComponentsBuilder.fromUri(uriCaptor.getValue()).build();
        assertThat(url.getQueryParams()).doesNotContainKey("uris");
    }

    @Test
    void getStat_shouldReturnEmptyListOnException() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 2, 0, 0, 0);
        List<String> uris = List.of("/events");
        Boolean unique = true;

        when(restClient.get()).thenThrow(new RuntimeException("Connection failed"));

        List<ViewStatsDto> result = statClient.getStat(start, end, uris, unique);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(restClient).get();
    }

    @Test
    void getStat_shouldFormatDateCorrectly() {
        LocalDateTime start = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        List<String> uris = List.of("/test");
        Boolean unique = true;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpecForGet);
        when(responseSpecForGet.body(any(ParameterizedTypeReference.class))).thenReturn(Collections.emptyList());

        statClient.getStat(start, end, uris, unique);

        verify(requestHeadersUriSpec).uri(uriCaptor.capture());

        UriComponents url = UriComponentsBuilder.fromUri(uriCaptor.getValue()).build();
        assertThat(decodeUrl(url, "start"))
                .isEqualTo("2024-12-31 23:59:59");

        assertThat(decodeUrl(url, "end"))
                .isEqualTo("2025-01-01 00:00:00");
    }

    @Test
    void getStat_shouldUseCorrectParameterizedTypeReference() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 2, 0, 0, 0);
        List<String> uris = List.of("/events");
        Boolean unique = false;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpecForGet);
        when(responseSpecForGet.body(any(ParameterizedTypeReference.class))).thenReturn(Collections.emptyList());

        statClient.getStat(start, end, uris, unique);

        verify(responseSpecForGet).body(typeReferenceCaptor.capture());
        ParameterizedTypeReference<List<ViewStatsDto>> capturedTypeRef = typeReferenceCaptor.getValue();

        assertThat(capturedTypeRef).isNotNull();
        assertThat(capturedTypeRef.getType().getTypeName()).contains("ViewStatsDto");
    }

    private String decodeUrl(UriComponents url, String key) {
        return URLDecoder.decode(
                url.getQueryParams().getFirst(key),
                StandardCharsets.UTF_8
        );
    }
}
