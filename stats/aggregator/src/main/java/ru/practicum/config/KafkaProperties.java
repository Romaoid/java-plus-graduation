package ru.practicum.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aggregator.kafka")
public class KafkaProperties {
    private Topics topics = new Topics();
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();

    @Data
    public static class Topics {
        private String actions;
        private String similarity;
    }

    @Data
    public static class Producer {
        private String bootstrapServers;
        private String keySerializer;
        private String valueSerializer;
    }

    @Data
    public static class Consumer {
        private String clientId;
        private String groupId;
        private String bootstrapServers;
        private String keyDeserializer;
        private String valueDeserializer;

        private int maxPollRecords;
        private int fetchMaxBytes;
        private int maxPartitionFetch;
        private int attemptTimeout;

        private boolean enableAutoCommit;
    }
}
