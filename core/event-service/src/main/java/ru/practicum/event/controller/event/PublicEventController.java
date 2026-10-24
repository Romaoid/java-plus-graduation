package ru.practicum.event.controller.event;

import jakarta.servlet.http.HttpServletRequest;
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

    @Loggable
    @GetMapping
    public List<EventShortDto> getEvents(@ModelAttribute EventSearchParams params, HttpServletRequest request) {
        return eventOrchestrationService.getPublicEvents(params, request);
    }

    @Loggable
    @GetMapping("/{id}")
    public EventFullDto getEventById(@PathVariable Long id, HttpServletRequest request) {
        return eventOrchestrationService.getPublicEventById(id, request);
    }
}