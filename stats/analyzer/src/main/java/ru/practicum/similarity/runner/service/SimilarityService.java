package ru.practicum.similarity.runner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.similarity.runner.dao.SimilarityRepository;
import ru.practicum.similarity.runner.entity.Similarity;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimilarityService {
    private final SimilarityRepository repository;

    @Transactional
    public void save(Collection<Similarity> incoming) {
        repository.saveAll(incoming);
    }

    public List<Similarity> getSimilaritiesByEventIds(Collection<Long> ids) {
        return repository.findByEventIds(ids);
    }

    public List<Similarity> getSimilaritiesByEventId(Long eventId) {
        return repository.findByEventId(eventId);
    }
}
