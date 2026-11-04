package ru.practicum.action.runner;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;
import ru.practicum.action.runner.consumer.ActionConsumer;
import ru.practicum.action.runner.entity.UserAction;
import ru.practicum.action.runner.service.ActionService;
import ru.practicum.config.KafkaProperties;
import ru.practicum.consumer.AbstractConsumerRunner;
import ru.practicum.consumer.BaseConsumer;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.util.*;

@Slf4j
@Component
public class ActionConsumerRunner extends AbstractConsumerRunner<Long, UserActionAvro> {

    private final ActionService service;

    public ActionConsumerRunner(BaseConsumer baseConsumer,
                                ActionConsumer config,
                                KafkaProperties properties,
                                ActionService service) {
        super(baseConsumer.create(config), properties.getTopics().getActionList());
        this.service = service;
    }

    @Override
    protected void handleBatch(ConsumerRecords<Long, UserActionAvro> records) {
        List<UserAction> entities = new ArrayList<>(records.count());
        records.forEach(record -> {
            UserActionAvro value = record.value();
            UserAction entity = new UserAction();
            entity.setEventId(value.getEventId());
            entity.setUserId(value.getUserId());
            entity.setRating(ratingOf(value.getActionType()));
            entity.setTimestamp(value.getTimestamp());
            entities.add(entity);
        });

        if (!entities.isEmpty()) {
            service.saveOrUpdate(entities);
            log.debug("Saved {} user actions", entities.size());
        }
    }

    private Double ratingOf(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}
