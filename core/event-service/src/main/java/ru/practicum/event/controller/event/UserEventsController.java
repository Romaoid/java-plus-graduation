package ru.practicum.event.controller.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.service.EventOrchestrationService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class UserEventsController {
    private final EventOrchestrationService eventOrchestrationService;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@Valid @RequestBody NewEventDto request,
                                 @PathVariable Long userId) {

        return eventOrchestrationService.addEvent(userId, request);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getOwnEvent(@PathVariable Long userId,
                                 @PathVariable Long eventId) {

        return eventOrchestrationService.getPrivateEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@Valid @RequestBody UpdateEventUserRequest request,
                                    @PathVariable Long userId,
                                    @PathVariable Long eventId) {

        return eventOrchestrationService.updateEvent(userId, eventId, request);
    }

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getOwnEvents(
            @PathVariable Long userId,
            @PositiveOrZero(message = "Field: from. Error: must be positive. Value: ${validatedValue}")
            @RequestParam(name = "from", required = false, defaultValue = "0") int from,
            @Positive(message = "Field: size. Error: must be positive. Value: ${validatedValue}")
            @RequestParam(name = "size", required = false, defaultValue = "10") int size) {

        return eventOrchestrationService.getPrivateEvents(userId, from, size);
    }
}