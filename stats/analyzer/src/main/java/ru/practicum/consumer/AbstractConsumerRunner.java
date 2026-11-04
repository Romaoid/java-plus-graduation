package ru.practicum.consumer;


import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class AbstractConsumerRunner<K, V> implements Runnable {

    private final Consumer<K, V> consumer;
    private final List<String> topics;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean inPoll = new AtomicBoolean(false);

    protected AbstractConsumerRunner(Consumer<K, V> consumer, List<String> topics) {
        this.consumer = consumer;
        this.topics = topics;
    }

    /** Обработка батча записей. Наследник сам решает, что делать. */
    protected abstract void handleBatch(ConsumerRecords<K, V> records);

    @Override
    public void run() {
        consumer.subscribe(topics);
        log.info("Consumer started, subscribed to {}", topics);
        try {
            while (running.get()) {
                inPoll.set(true);
                ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(500));
                inPoll.set(false);

                if (!records.isEmpty()) {
                    try {
                        handleBatch(records);
                        consumer.commitSync();
                    } catch (Exception ex) {
                        log.error("Ошибка обработки батча, оффсеты не закоммичены", ex);
                        // при желании: retry / DLQ / проброс
                    }
                }
            }
        } catch (WakeupException e) {
            if (running.get()) {
                throw e;
            }
            log.info("Consumer wakeup received, stopping...");
        } finally {
            try {
                consumer.commitSync();
            } catch (Exception ex) {
                log.warn("Не удалось закоммитить оффсеты при остановке", ex);
            } finally {
                consumer.close();
                log.info("Consumer closed");
            }
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping consumer...");
        running.set(false);
        if (inPoll.get()) {
            consumer.wakeup();
        }
    }
}
