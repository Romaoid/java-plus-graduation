package ru.practicum.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@RequiredArgsConstructor
@Component
public class BaseConsumer {
    public <K, V> Consumer<K, V> create(ConsumerConfigProvider configProvider) {
        Properties props = new Properties();

        props.put(ConsumerConfig.CLIENT_ID_CONFIG, configProvider.getClientId());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, configProvider.getGroupId());
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, configProvider.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, configProvider.getKeyDeserializer());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, configProvider.getValueDeserializer());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, configProvider.getMaxPollRecords());
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, configProvider.getFetchMaxBytes());
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, configProvider.getMaxPartitionFetch());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, configProvider.getEnableAutoCommit());

        log.info("Initializing Kafka consumer with bootstrap servers: {}",
                configProvider.getBootstrapServers());
        log.debug("Kafka consumer config: {}", props);

        return new KafkaConsumer<>(props);
    }
}
