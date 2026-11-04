package ru.practicum.action.runner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.action.runner.dao.ActionRepository;
import ru.practicum.action.runner.entity.EventInteractionCount;
import ru.practicum.action.runner.entity.UserAction;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionService {
    private final ActionRepository repository;

    public List<UserAction> getActionsByUserId(Long id) {
        return repository.findByUserId(id);
    }

    public List<UserAction> getActionsByEventIdsEndUserId(Collection<Long> eventIds,Long userId) {
        return repository.findByEventIdInAndUserId(eventIds, userId);
    }

    public List<UserAction> getLastNActionsByUserId(long userId, long n) {
        return repository.findLastUserActionsWithLimitN(userId, n);
    }

    public List<EventInteractionCount> getInteractionsCount(Collection<Long> eventIds) {
        return repository.getInteractionsCount(eventIds);
    }

    @Transactional
    public void saveOrUpdate(List<UserAction> incoming) {
        Map<ActionKey, UserAction> byKey = new LinkedHashMap<>();
        for (UserAction action : incoming) {
            byKey.merge(new ActionKey(action.getEventId(), action.getUserId()),
                    action,
                    (oldOne, newOne) -> newOne.getRating() > oldOne.getRating() ? newOne : oldOne);
        }
        if (byKey.isEmpty()) {
            return;
        }

        Set<Long> eventIds = byKey.keySet().stream().map(ActionKey::eventId).collect(Collectors.toSet());
        Set<Long> userIds = byKey.keySet().stream().map(ActionKey::userId).collect(Collectors.toSet());
        List<UserAction> existing = repository.findByEventIdInAndUserIdIn(eventIds, userIds);

        Map<ActionKey, UserAction> existingByKey = existing.stream()
                .collect(Collectors.toMap(
                        a -> new ActionKey(a.getEventId(), a.getUserId()),
                        a -> a));

        List<UserAction> toSave = new ArrayList<>(byKey.size());
        byKey.forEach((key, fresh) -> {
            UserAction old = existingByKey.get(key);
            if (old == null) {
                toSave.add(fresh);
            } else if (fresh.getRating() > old.getRating()) {
                old.setRating(fresh.getRating());
                old.setTimestamp(fresh.getTimestamp());
                toSave.add(old);
            }
        });

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
            log.debug("Upserted {} actions ({} keys in batch, {} skipped)",
                    toSave.size(), byKey.size(), byKey.size() - toSave.size());
        }
    }

    private record ActionKey(Long eventId, Long userId) {}
}
