package ru.practicum.comment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.EventDto;
import ru.practicum.comment.fallback.factory.EventClientFallbackFactory;

import java.util.List;
import java.util.Set;

@FeignClient(name = "event-service", fallbackFactory = EventClientFallbackFactory.class)
public interface EventClient {

    @GetMapping("/api/events/{id}")
    EventDto getEventById(@PathVariable("id") Long id);

    @PostMapping("/api/events")
    List<EventDto> getEventsByIds(@RequestBody Set<Long> ids);
}
