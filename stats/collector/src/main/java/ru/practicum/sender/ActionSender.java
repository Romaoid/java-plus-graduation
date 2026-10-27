package ru.practicum.sender;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaProperties;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.producer.ProducerClient;

@Slf4j
@Component
public class ActionSender {

    private final ProducerClient client;
    private final String topic;

    public ActionSender(ProducerClient client, KafkaProperties properties) {
        this.client = client;
        this.topic = properties.getTopics().getActions();
    }

    public void sendAction(UserActionAvro message) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, message);
        client.getProducer().send(record, (metadata, ex) -> {
            if (ex != null) {
                log.error("Ошибка отправки в Kafka", ex);
            } else {
                log.debug("Отправлено в {} partition={} offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    public void stop() {
        client.stop();
    }
}
