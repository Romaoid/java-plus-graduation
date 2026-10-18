package ru.practicum.event.service;

import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;

import java.util.List;
import java.util.Set;

public interface EventService {

    Event addEvent(Long initiatorId, NewEventDto dto);

    Event getPrivateEvent(Long userId, Long eventId);

    Event updateEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<Event> getPrivateEvents(long userId, int from, int size);

    List<Event> getPublicEvents(EventSearchParams params);

    Event getPublicEventById(Long id);

    List<Event> getEventsByAdmin(AdminEventSearchParams params);

    Event updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    List<Event> findAllByIdIn(Set<Long> ids);
}