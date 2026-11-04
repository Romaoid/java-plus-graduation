package ru.practicum.similarity.runner.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.similarity.runner.entity.Similarity;

import java.util.Collection;
import java.util.List;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {

    @Query("SELECT s FROM " +
            "Similarity s " +
            "WHERE s.eventA IN :ids " +
            "OR s.eventB IN :ids")
    List<Similarity> findByEventIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT s " +
            "FROM Similarity s " +
            "WHERE s.eventA = :eventId " +
            "OR s.eventB = :eventId")
    List<Similarity> findByEventId(@Param("eventId") Long eventId);
}
