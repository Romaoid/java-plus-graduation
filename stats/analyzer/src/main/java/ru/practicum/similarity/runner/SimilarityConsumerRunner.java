package ru.practicum.similarity.runner;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaProperties;
import ru.practicum.consumer.AbstractConsumerRunner;
import ru.practicum.consumer.BaseConsumer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.similarity.runner.consumer.SimilarityConsumer;
import ru.practicum.similarity.runner.entity.Similarity;
import ru.practicum.similarity.runner.service.SimilarityService;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class SimilarityConsumerRunner extends AbstractConsumerRunner<Long, EventSimilarityAvro> {
    private final SimilarityService service;

    public SimilarityConsumerRunner(BaseConsumer baseConsumer,
                                    SimilarityConsumer config,
                                    KafkaProperties properties,
                                    SimilarityService service) {
        super(baseConsumer.create(config), properties.getTopics().getSimilarityList());
        this.service = service;
    }

    @Override
    protected void handleBatch(ConsumerRecords<Long, EventSimilarityAvro> records) {
        Map<SimilarityKey, Similarity> byKey = new LinkedHashMap<>();

        records.forEach(record -> {
            EventSimilarityAvro value = record.value();
            Similarity entity = new Similarity();
            entity.setEventA(value.getEventA());
            entity.setEventB(value.getEventB());
            entity.setSimilarity(value.getScore());
            entity.setTimestamp(value.getTimestamp());

            byKey.merge(new SimilarityKey(value.getEventA(), value.getEventB()),
                    entity,
                    (oldOne, newOne) -> newOne.getTimestamp().isAfter(oldOne.getTimestamp())
                            ? newOne : oldOne);
        });

        if (!byKey.isEmpty()) {
            service.save(byKey.values());
            log.debug("Saved {} similarities", byKey.size());
        }
    }

    private record SimilarityKey(Long eventA, Long eventB) {}
}
