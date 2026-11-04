package ru.practicum.action.runner.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.action.runner.entity.EventInteractionCount;
import ru.practicum.action.runner.entity.UserAction;

import java.util.Collection;
import java.util.List;

public interface ActionRepository extends JpaRepository<UserAction,Long> {

    List<UserAction> findByUserId(Long userId);

    List<UserAction> findByEventIdInAndUserId(Collection<Long> eventIds, Long userId);

    List<UserAction> findByEventIdInAndUserIdIn(Collection<Long> eventIds, Collection<Long> userIds);

    @Query(value = "SELECT * " +
            "FROM actions as a " +
            "WHERE a.user_id = :userId " +
            "ORDER BY a.ts DESC " +
            "LIMIT :max",
            nativeQuery = true)
    List<UserAction> findLastUserActionsWithLimitN(@Param("userId") long userId, @Param("max") long max);

    @Query("SELECT a.eventId as event_id, " +
            "SUM(a.rating) as score " +
            "FROM UserAction as a " +
            "WHERE a.eventId IN :eventIds " +
            "GROUP BY a.eventId")
    List<EventInteractionCount> getInteractionsCount(@Param("eventIds") Collection<Long> eventIds);


}
