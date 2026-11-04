package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.action.runner.entity.UserAction;
import ru.practicum.action.runner.service.ActionService;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.similarity.runner.entity.Similarity;
import ru.practicum.similarity.runner.service.SimilarityService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AnalyzerOrchestrationService {

    private final ActionService actionService;
    private final SimilarityService similarityService;

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId,long maxResults) {
        // 1. недавно просмотренные (top-N по ts)
        List<UserAction> recent = actionService.getLastNActionsByUserId(userId, maxResults);
        if (recent.isEmpty()) {
            return Stream.empty();
        }

        List<Long> historyIds  = recent.stream().map(UserAction::getEventId).toList();

        // 2. top-N похожих на них, с которыми пользователь не взаимодействовал
        List<Similarity> topN = similarityService.getSimilaritiesByEventIds(historyIds)
                .stream()
                .filter(s -> !bothInHistory(s, historyIds ))
                .sorted(Comparator.comparingDouble(Similarity::getSimilarity).reversed())
                .limit(maxResults)
                .toList();
        if (topN.isEmpty()) {
            return Stream.empty();
        }

        List<Long> recommendedIds = topN.stream()
                .map(s -> historyIds.contains(s.getEventA()) ? s.getEventB() : s.getEventA())
                .toList();

        // 3. для каждого — список ближайших соседей и список пользовательских оценок
        List<Similarity> simsForRecommended = similarityService.getSimilaritiesByEventIds(recommendedIds);

        Map<Long, List<Similarity>> simsByEvent = simsForRecommended.stream()
                .flatMap(s -> Stream.of(
                        Map.entry(s.getEventA(), s),
                        Map.entry(s.getEventB(), s)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        Set<Long> neighbourIds = simsForRecommended.stream()
                .flatMap(s -> Stream.of(s.getEventA(), s.getEventB()))
                .collect(Collectors.toSet());

        Map<Long, Double> ratings = actionService.getActionsByEventIdsEndUserId(neighbourIds, userId)
                .stream()
                .collect(Collectors.toMap(UserAction::getEventId, UserAction::getRating));

        // 4. Stream<RecommendedEventProto> с event_id и score (предсказанная оценка)
        return recommendedIds.stream()
                .distinct()
                .map(id -> RecommendedEventProto.newBuilder()
                        .setEventId(id)
                        .setScore(getPredictedScore(id, simsByEvent.get(id), ratings))
                        .build());
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, long maxResults) {
        List<Similarity> similarList = similarityService.getSimilaritiesByEventId(eventId);
        List<UserAction>  actionsList = actionService.getActionsByUserId(userId);
        List<Long> interactionEventIds = actionsList.stream().map(UserAction::getEventId).toList();

        return similarList.stream()
                .filter(s -> (!interactionEventIds.contains(s.getEventA()) && !interactionEventIds.contains(s.getEventB())))
                .sorted(Comparator.comparingDouble(Similarity::getSimilarity).reversed())
                .limit(maxResults)
                .map(s -> RecommendedEventProto.newBuilder()
                        .setEventId(s.getEventA() == eventId
                                ? s.getEventB()
                                : s.getEventA())
                        .setScore(s.getSimilarity())
                        .build()
                );
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Stream.empty();
        }
        return actionService.getInteractionsCount(eventIds).stream()
                .map(a -> RecommendedEventProto.newBuilder()
                        .setEventId(a.getEventId())
                        .setScore(a.getScore())
                        .build());
    }

    private Boolean bothInHistory(Similarity similarity, List<Long> ids) {
        return ids.contains(similarity.getEventA()) && ids.contains(similarity.getEventB());
    }

    private double getPredictedScore(long eventId, List<Similarity> sims, Map<Long, Double> ratings) {
        double weightedScoresSum = 0.0;
        double similarityCoefficientsSum = 0.0;

        for (Similarity s : sims) {
            Long neighbour = s.getEventA() == eventId ? s.getEventB() : s.getEventA();
            double rating = ratings.get(neighbour);
            weightedScoresSum += s.getSimilarity() * rating;
            similarityCoefficientsSum += s.getSimilarity();
        }

        return weightedScoresSum / similarityCoefficientsSum;
    }
}
