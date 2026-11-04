package ru.practicum.action.runner.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.consumer.ConsumerConfigProvider;
import ru.practicum.config.KafkaProperties;


@Slf4j
@Component
@RequiredArgsConstructor
public class ActionConsumer implements ConsumerConfigProvider {
    private final KafkaProperties kafkaProperties;

    @Override
    public String getClientId() {
        return kafkaProperties.getActionConsumer().getClientId();
    }

    @Override
    public String getGroupId() {
        return kafkaProperties.getActionConsumer().getGroupId();
    }

    @Override
    public String getBootstrapServers() {
        return kafkaProperties.getActionConsumer().getBootstrapServers();
    }

    @Override
    public String getKeyDeserializer() {
        return kafkaProperties.getActionConsumer().getKeyDeserializer();
    }

    @Override
    public String getValueDeserializer() {
        return kafkaProperties.getActionConsumer().getValueDeserializer();
    }

    @Override
    public int getMaxPollRecords() {
        return kafkaProperties.getActionConsumer().getMaxPollRecords();
    }

    @Override
    public int getFetchMaxBytes() {
        return kafkaProperties.getActionConsumer().getFetchMaxBytes();
    }

    @Override
    public int getMaxPartitionFetch() {
        return kafkaProperties.getActionConsumer().getMaxPartitionFetch();
    }

    @Override
    public boolean getEnableAutoCommit() {
        return kafkaProperties.getActionConsumer().getEnableAutoCommit(); }
}
