package ru.practicum.event.controller.event;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventSearchParams;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.logger.Loggable;
import ru.practicum.event.service.EventOrchestrationService;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventOrchestrationService eventOrchestrationService;
    private final String HEADER_USER_ID = "X-EWM-USER-ID";

    @Loggable
    @GetMapping
    public List<EventShortDto> getEvents(@ModelAttribute EventSearchParams params) {
        return eventOrchestrationService.getPublicEvents(params);
    }

    @Loggable
    @GetMapping("/{id}")
    public EventFullDto getEventById(@RequestHeader(value = HEADER_USER_ID, required = false, defaultValue = "-1") long userId,
                                     @PathVariable Long id) {

        return eventOrchestrationService.getEventById(id, userId);
    }

    @PutMapping("/{eventId}/like")
    public void putLike(@RequestHeader(value = HEADER_USER_ID) long userId,
                        @PathVariable Long eventId) {
        eventOrchestrationService.sendLike(eventId, userId);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(@RequestHeader(value = HEADER_USER_ID) long userId,
                                                  @RequestParam(name = "max", required = false, defaultValue = "10") int max) {
        return eventOrchestrationService.getRecommendationsByUserId(userId, max);
    }

    @GetMapping("/{eventId}/similar")
    public List<EventShortDto> gerSimilarEvents(@PathVariable long eventId,
                                                @RequestHeader(value = HEADER_USER_ID) long userId,
                                                @RequestParam(name = "max", required = false, defaultValue = "10") int max) {
        return eventOrchestrationService.getSimilarEvents(eventId, userId, max);
    }
}