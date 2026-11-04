package ru.practicum.similarity.runner.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaProperties;
import ru.practicum.consumer.ConsumerConfigProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityConsumer implements ConsumerConfigProvider {
    private final KafkaProperties kafkaProperties;

    @Override
    public String getClientId() {
        return kafkaProperties.getSimilarityConsumer().getClientId();
    }

    @Override
    public String getGroupId() {
        return kafkaProperties.getSimilarityConsumer().getGroupId();
    }

    @Override
    public String getBootstrapServers() {
        return kafkaProperties.getSimilarityConsumer().getBootstrapServers();
    }

    @Override
    public String getKeyDeserializer() {
        return kafkaProperties.getSimilarityConsumer().getKeyDeserializer();
    }

    @Override
    public String getValueDeserializer() {
        return kafkaProperties.getSimilarityConsumer().getValueDeserializer();
    }

    @Override
    public int getMaxPollRecords() {
        return kafkaProperties.getSimilarityConsumer().getMaxPollRecords();
    }

    @Override
    public int getFetchMaxBytes() {
        return kafkaProperties.getSimilarityConsumer().getFetchMaxBytes();
    }

    @Override
    public int getMaxPartitionFetch() {
        return kafkaProperties.getSimilarityConsumer().getMaxPartitionFetch();
    }

    @Override
    public boolean getEnableAutoCommit() {
        return kafkaProperties.getSimilarityConsumer().getEnableAutoCommit(); }

}
