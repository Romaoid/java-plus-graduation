package ru.practicum.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@EnableCaching
@EnableFeignClients
@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.event", "ru.practicum.stats"})
public class EventApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(EventApplication.class, args);
    }
}
