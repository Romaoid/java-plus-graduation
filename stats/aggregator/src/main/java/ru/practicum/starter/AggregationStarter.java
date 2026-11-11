package ru.practicum.starter;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaProperties;
import ru.practicum.consumer.ActionConsumer;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.sender.SimilaritySender;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class AggregationStarter {
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final SimilarityState state;

    private final SimilaritySender producer;
    private final Consumer<Void, UserActionAvro> consumer;
    private final Duration consumeAttemptTimeout;
    private final String consumerTopic;

    public AggregationStarter(SimilaritySender producer, ActionConsumer consumer,
                              KafkaProperties properties, SimilarityState state) {
        this.state = state;
        this.producer = producer;
        this.consumer = consumer.create(properties);
        this.consumeAttemptTimeout = Duration.ofMillis(properties.getConsumer().getAttemptTimeout());
        this.consumerTopic = properties.getTopics().getActions();
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(Collections.singleton(consumerTopic));

            while (true) {
                ConsumerRecords<Void, UserActionAvro> records = consumer.poll(consumeAttemptTimeout);

                int count = 0;
                for (ConsumerRecord<Void, UserActionAvro> record : records) {
                    Optional<List<EventSimilarityAvro>> calculatedEventSimilarity = processRecord(record.value());

                    calculatedEventSimilarity.ifPresent(list -> {
                        for (EventSimilarityAvro i : list) {
                            producer.send(i);
                        }
                    });

                    count++;
                    manageOffsets(record, count, consumer);
                }

                consumer.commitAsync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время вычисления похожих событий", e);
        } finally {

            try {
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.stop();
            }
        }
    }

    private void manageOffsets(ConsumerRecord<Void, UserActionAvro> record,
                               int count, Consumer<Void, UserActionAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private Optional<List<EventSimilarityAvro>> processRecord(UserActionAvro action) {
        final MathContext SQRT_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
        long eventA = action.getEventId();
        long userId = action.getUserId();
        BigDecimal newWeight = weightOf(action.getActionType());
        BigDecimal oldWeight = state.weight(eventA, userId);

        if (!state.updateWeight(eventA, userId, newWeight)) {
            return Optional.empty();
        }

        Set<Long> eventIds = state.eventsForUser(userId);
        eventIds.remove(eventA);
        if (eventIds.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal sA = state.totalWeight(eventA);
        List<EventSimilarityAvro> out = new ArrayList<>(eventIds.size());

        for (long eventB : eventIds) {
            BigDecimal weightB = state.weight(eventB, userId);
            BigDecimal deltaMin = newWeight.min(weightB).subtract((oldWeight.min(weightB)));
            BigDecimal sMinNew = state.sMin(eventA, eventB).add(deltaMin);
            if (deltaMin.compareTo(new BigDecimal("0.0")) != 0) {
                state.setSMin(eventA, eventB, sMinNew);
            }

            BigDecimal sB = state.totalWeight(eventB);
            BigDecimal sqrtSA = sA.sqrt(SQRT_CONTEXT);
            BigDecimal sqrtSB = sB.sqrt(SQRT_CONTEXT);
            BigDecimal multiplySqrtTotalWeights = sqrtSA.multiply(sqrtSB);
            BigDecimal similarity = sMinNew.divide(multiplySqrtTotalWeights, SQRT_CONTEXT);
            out.add(EventSimilarityAvro.newBuilder()
                    .setEventA(Math.min(eventA, eventB))
                    .setEventB(Math.max(eventA, eventB))
                    .setScore(similarity.doubleValue())
                    .setTimestamp(action.getTimestamp())
                    .build());
        }

        return Optional.of(out);
    }

    private static BigDecimal weightOf(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> new BigDecimal("0.4");
            case REGISTER -> new BigDecimal("0.8");
            case LIKE -> new BigDecimal("1.0");
        };
    }
}
