package ru.practicum.comment.fallback.factory;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.comment.dto.EventDto;
import ru.practicum.comment.exception.NotFoundException;
import ru.practicum.comment.exception.ProcessingException;
import ru.practicum.comment.feign.EventClient;

import java.util.List;
import java.util.Set;

@Component
public class EventClientFallbackFactory implements FallbackFactory<EventClient> {
    private static final Logger log = LoggerFactory.getLogger(EventClientFallbackFactory.class);

    @Override
    public EventClient create(Throwable cause) {
        return new EventClient() {
            @Override
            public EventDto getEventById(Long id) {
                throw translate(cause);
            }

            @Override
            public List<EventDto> getEventsByIds(Set<Long> ids) {
                throw translate(cause);
            }
        };
    }

    private RuntimeException translate(Throwable cause) {
        if (cause instanceof FeignException feignException
                && feignException.status() >= 400 && feignException.status() < 500) {
            log.warn("Event service returned client error: {}", feignException.status());
            return new NotFoundException(feignException.getMessage());
        }
        log.warn("Event service unavailable: {}", cause.toString());
        return new ProcessingException("Event service unavailable");
    }
}
