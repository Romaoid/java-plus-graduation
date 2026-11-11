package ru.practicum.similarity.runner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.similarity.runner.dao.SimilarityRepository;
import ru.practicum.similarity.runner.entity.Similarity;

import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
public class SimilarityService {
    private final SimilarityRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final String upsertSql;

    public SimilarityService(SimilarityRepository repository, JdbcTemplate jdbcTemplate,
                             @Value("${analyzer.db.upsert-sql:ON_CONFLICT}") String mode) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        String sqlPostgres = """
              INSERT INTO similarities (event_a, event_b, similarity, ts)
              VALUES (?, ?, ?, ?)
              ON CONFLICT (event_a, event_b)
              DO UPDATE SET similarity = EXCLUDED.similarity, ts = EXCLUDED.ts
              """;
        String sqlH2 = "MERGE INTO similarities (event_a, event_b, similarity, ts) KEY (event_a, event_b) VALUES (?, ?, ?, ?)";

        this.upsertSql = "MERGE".equalsIgnoreCase(mode) ? sqlH2 : sqlPostgres;
    }

    @Transactional
    public void save(Collection<Similarity> incoming) {
        jdbcTemplate.batchUpdate(upsertSql, incoming.stream()
                .map(s -> new Object[]{s.getEventA(), s.getEventB(), s.getSimilarity(), s.getTimestamp().atOffset(ZoneOffset.UTC)})
                .toList());
    }

    public List<Similarity> getSimilaritiesByEventIds(Collection<Long> ids) {
        return repository.findByEventIds(ids);
    }

    public List<Similarity> getSimilaritiesByEventId(Long eventId) {
        return repository.findByEventId(eventId);
    }
}
