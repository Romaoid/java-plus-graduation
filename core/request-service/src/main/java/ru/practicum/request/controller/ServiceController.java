package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestOrchestrationService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ServiceController {
    private final RequestOrchestrationService requestService;

    @GetMapping("/users/{userId}/event/{eventId}/request/status")
    public ParticipationRequestDto getRequestStatus(@PathVariable("userId") Long userId,
                                                    @PathVariable("eventId") Long eventId) {
        return requestService.getRequestStatusByIds(eventId, userId);
    }
}
