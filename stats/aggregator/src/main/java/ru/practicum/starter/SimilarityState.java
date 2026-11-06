package ru.practicum.starter;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory состояние для потокового расчёта косинусного сходства.
 *
 * Инварианты:
 *  1. usersWeightMap[e][u]   — максимальный вес действия пользователя u с событием e (0..1).
 *  2. totalWeightSum[e]      == sum(usersWeightMap[e].values()).
 *  3. minWeightsSum хранится только в нормализованном порядке: ключ — min(a,b), подклбч — max(a,b).
 *  4. minWeightsSum[a][b]    == sum_u min(usersWeightMap[a][u], usersWeightMap[b][u]).
 */
@Component
public class SimilarityState {

    /** eventId -> (userId -> weight) */
    private final Map<Long, Map<Long, Double>> usersWeightMap = new HashMap<>();
    /** minEventId -> (maxEventId -> S_min) */
    private final Map<Long, Map<Long, Double>> minWeightsSum = new HashMap<>();
    /** eventId -> S_event */
    private final Map<Long, Double> totalWeightSum = new HashMap<>();

    /**
     * Обновляет вес пользователя для события.
     *
     * @return true, если вес реально вырос (и состояние изменилось);
     *         false, если новое действие не увеличивает вес (VIEW после LIKE и т.п.).
     */
    public boolean updateWeight(long eventId, long userId, double newWeight) {
        Map<Long, Double> users = usersWeightMap.computeIfAbsent(eventId, e -> new HashMap<>());
        double oldWeight = users.getOrDefault(userId, 0.0);

        if (newWeight <= oldWeight) {
            return false;
        }

        users.put(userId, newWeight);
        totalWeightSum.merge(eventId, newWeight - oldWeight, Double::sum);
        return true;
    }

    public double weight(long eventId, long userId) {
        return usersWeightMap.getOrDefault(eventId, Map.of()).getOrDefault(userId, 0.0);
    }

    /** Все события, с которыми у пользователя есть ненулевой вес. */
    public Set<Long> eventsForUser(long userId) {
        Set<Long> result = new HashSet<>();
        for (Map.Entry<Long, Map<Long, Double>> e : usersWeightMap.entrySet()) {
            if (e.getValue().containsKey(userId)) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    public double totalWeight(long eventId) {
        return totalWeightSum.getOrDefault(eventId, 0.0);
    }

    public double sMin(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightsSum.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
    }

    public void setSMin(long eventA, long eventB, double value) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightsSum.computeIfAbsent(first, e -> new HashMap<>()).put(second, value);
    }

}
