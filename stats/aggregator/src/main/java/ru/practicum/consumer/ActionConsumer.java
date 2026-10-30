package ru.practicum.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaProperties;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Properties;

@Slf4j
@Component
public class ActionConsumer {

    public Consumer<Void, UserActionAvro> create(KafkaProperties properties) {
        Properties props = new Properties();
        KafkaProperties.Consumer consumerConf = properties.getConsumer();

        props.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerConf.getClientId());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerConf.getGroupId());
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerConf.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, consumerConf.getKeyDeserializer());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, consumerConf.getValueDeserializer());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerConf.getMaxPollRecords());
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, consumerConf.getFetchMaxBytes());
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, consumerConf.getMaxPartitionFetch());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerConf.isEnableAutoCommit());

        log.info("Initializing Kafka consumer with bootstrap servers: {}",
                consumerConf.getBootstrapServers());
        log.debug("Kafka consumer config: {}", props);

        return new KafkaConsumer<>(props);
    }
}
