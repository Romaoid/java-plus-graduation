package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.action.runner.ActionConsumerRunner;
import ru.practicum.similarity.runner.SimilarityConsumerRunner;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Analyzer {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Analyzer.class, args);

        ActionConsumerRunner actionRunner = context.getBean(ActionConsumerRunner.class);
        SimilarityConsumerRunner similarityRunner = context.getBean(SimilarityConsumerRunner.class);

        Thread actionThread = new Thread(actionRunner, "action-consumer");
        Thread similarityThread = new Thread(similarityRunner, "similarity-consumer");
        actionThread.start();
        similarityThread.start();
    }
}
