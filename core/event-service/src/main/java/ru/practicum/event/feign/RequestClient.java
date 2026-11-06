package ru.practicum.event.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.event.dto.ParticipationRequestDto;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/api/users/{userId}/event/{eventId}/request/status")
    ParticipationRequestDto getRequestStatus(@PathVariable("userId") Long userId,
                                    @PathVariable("eventId") Long eventId);
}
