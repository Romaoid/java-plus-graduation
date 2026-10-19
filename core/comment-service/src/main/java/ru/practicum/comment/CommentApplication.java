package ru.practicum.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

@EnableCaching
@EnableFeignClients
@SpringBootApplication
public class CommentApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CommentApplication.class, args);
    }
}
