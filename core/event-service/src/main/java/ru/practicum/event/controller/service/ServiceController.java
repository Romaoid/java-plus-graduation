package ru.practicum.event.controller.service;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventDto;
import ru.practicum.event.service.EventOrchestrationService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class ServiceController {
    private final EventOrchestrationService orchestrationService;

    @GetMapping("/{id}")
    public EventDto getEventById(@PathVariable long id) {
        return orchestrationService.getEventByIdByApi(id);
    }

    @PostMapping("/{id}")
    public void updateConfirmedRequestsByEventId(@PathVariable long id,
                                                        @RequestParam("modify") String modify) {
        orchestrationService.updateConfirmedRequestsByEventId(id, modify);
    }

    @PutMapping()
    public EventDto updateConfirmedRequestsByOwner(@RequestBody EventDto eventDto) {
        return orchestrationService.updateConfirmedRequestsByOwner(eventDto);
    }

    @PostMapping
    public List<EventDto> getEventsByIds(@RequestBody Set<Long> ids) {
        return orchestrationService.getEventsByIds(ids);
    }
}
