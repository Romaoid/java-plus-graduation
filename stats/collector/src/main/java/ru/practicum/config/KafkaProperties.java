package ru.practicum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "collector.kafka")
public class KafkaProperties {
    private Topics topics = new Topics();
    private Producer producer = new Producer();

    @Data
    public static class Topics {
        private String actions;
    }

    @Data
    public static class Producer {
        private String bootstrapServers;
        private String keySerializer;
        private String valueSerializer;
    }
}
