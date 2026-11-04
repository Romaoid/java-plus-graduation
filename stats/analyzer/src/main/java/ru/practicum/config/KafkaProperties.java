package ru.practicum.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "analyzer.kafka")
public class KafkaProperties {
    private Topics topics = new Topics();
    private ActionConsumer actionConsumer = new ActionConsumer();
    private SimilarityConsumer similarityConsumer = new SimilarityConsumer();
    @Getter
    private int consumerAttemptTimeout;

    @Data
    public static class Topics {
        private String action;
        private String similarity;

        public List<String> getSimilarityList() {
            return Arrays.asList(similarity.split(","));
        }

        public List<String> getActionList() {
            return Arrays.asList(action.split(","));
        }
    }

    @Data
    public static class ActionConsumer {
        private String bootstrapServers;
        private String clientId;
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
        private Integer maxPollRecords;
        private Integer fetchMaxBytes;
        private Integer maxPartitionFetch;
        private Boolean enableAutoCommit;
    }

    @Data
    public static class SimilarityConsumer {
        private String bootstrapServers;
        private String clientId;
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
        private Integer maxPollRecords;
        private Integer fetchMaxBytes;
        private Integer maxPartitionFetch;
        private Boolean enableAutoCommit;
    }
}
