package ru.practicum.request.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.EventDto;


@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/api/events/{id}")
    EventDto getEventById(@PathVariable("id") Long id);

    @PostMapping("/api/events/{id}")
    void updateConfirmedRequestsFromEventById(@PathVariable long id,
                                                    @RequestParam("modify") String modify);

    @PostMapping("/api/events")
    EventDto updateConfirmedRequestsFromEventByIdByOwner(@RequestBody EventDto dto);
}
