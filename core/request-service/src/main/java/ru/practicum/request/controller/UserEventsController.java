package ru.practicum.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestOrchestrationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class UserEventsController {

    private final RequestOrchestrationService requestService;

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getOwnParticipationRequests(@PathVariable Long userId, @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests", userId, eventId);

        return requestService.getRequestsByEventId(userId, eventId);
    }

    @PatchMapping()
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult updateRequestsToOwnEvent(@PathVariable Long userId,
                                                                   @PathVariable Long eventId,
                                                                   @Valid @RequestBody
                                                                   EventRequestStatusUpdateRequest request) {
        log.info("PATCH /users/{}/events/{}/requests with body: {}", userId, eventId, request);

        return requestService.updateOwnParticipationRequests(userId, eventId, request);
    }


}