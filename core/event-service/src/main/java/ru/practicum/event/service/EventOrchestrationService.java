package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.event.dto.*;
import ru.practicum.event.exception.ConflictException;
import ru.practicum.event.exception.NotFoundException;
import ru.practicum.event.exception.ProcessingException;
import ru.practicum.event.exception.ValidationException;
import ru.practicum.event.logger.Loggable;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventSort;
import ru.practicum.event.model.EventState;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventOrchestrationService {
    private final EventService service;
    private final UserService userService;
    private final StatClient statClient;

    @Loggable
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        UserShortDto initiator = userService.getUser(userId);

        Event event = service.addEvent(initiator.getId(), dto);

        return EventMapper.toFullDto(event, initiator, 0L);
    }

    @Loggable
    public EventFullDto getPrivateEvent(Long userId, Long eventId) {
        Event event = service.getPrivateEvent(userId, eventId);

        UserShortDto initiator = userService.getUser(userId);

        ViewStatsDto stat = getStatByEvent(event, false);

        return EventMapper.toFullDto(event, initiator, stat.getHits());
    }

    @Loggable
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = service.updateEvent(userId, eventId, request);

        UserShortDto initiator = userService.getUser(userId);

        ViewStatsDto stat = getStatByEvent(event, false);

        return EventMapper.toFullDto(event, initiator, stat.getHits());
    }

    @Loggable
    public List<EventShortDto> getPrivateEvents(long userId, int from, int size) {
        UserShortDto initiator;
        try {
            initiator = userService.getUser(userId);
        } catch (NotFoundException e) {
            throw new ValidationException("Field: userId. Error: id не найден. Value: " + userId);
        }

        List<Event> events = service.getPrivateEvents(userId, from, size);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .toList();

        Map<Long, Long> stats = getStatsByUris(uris);

        return events.stream()
                .map(view -> EventMapper.toShortDto(view, initiator, stats.get(view.getId())))
                .toList();
    }

    @Loggable
    public List<EventShortDto> getPublicEvents(EventSearchParams params, HttpServletRequest request) {
        List<Event> events = service.getPublicEvents(params);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .toList();

        Map<Long, Long> stats = getStatsByUris(uris);

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiator)
                .collect(Collectors.toSet());

        Map<Long, UserShortDto> initiators = userService.getUsers(initiatorIds).stream()
                .collect(Collectors.toMap(
                        UserShortDto::getId,
                        userShortDto -> userShortDto
                ));

        List<EventShortDto> result = events.stream()
                .map(view -> EventMapper.toShortDto(
                        view,
                        initiators.get(view.getInitiator()),
                        stats.get(view.getId())
                ))
                .toList();

        sendStat(request.getRequestURI(), request.getRemoteAddr());

        if (params.getSort() == EventSort.VIEWS) {
            return result.stream()
                    .sorted(
                            Comparator.comparingLong(EventShortDto::getViews)
                                    .reversed()
                    )
                    .toList();
        }

        return result;
    }

    @Loggable
    public EventFullDto getPublicEventById(Long id, HttpServletRequest request) {
        Event event = service.getPublicEventById(id);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + id + " не найдено");
        }

        UserShortDto initiator = userService.getUser(event.getInitiator());

        sendStat("/events/" + id, request.getRemoteAddr());

        ViewStatsDto stat = getStatByEvent(event, true);

        return EventMapper.toFullDto(event, initiator, stat.getHits());
    }

    @Loggable
    public List<EventFullDto> getEventsByAdmin(AdminEventSearchParams params) {
        List<Event> events = service.getEventsByAdmin(params);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .toList();

        Map<Long, Long> stats = getStatsByUris(uris);

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiator)
                .collect(Collectors.toSet());

        Map<Long, UserShortDto> initiators = userService.getUsers(initiatorIds).stream()
                .collect(Collectors.toMap(
                        UserShortDto::getId,
                        UserShortDto -> UserShortDto
                ));

        return events.stream()
                .map(view -> EventMapper.toFullDto(
                        view,
                        initiators.get(view.getInitiator()),
                        stats.get(view.getId())))
                .toList();
    }

    @Loggable
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request){
        System.out.println(request);
        Event event = service.updateEventByAdmin(eventId, request);

        System.out.println(event);
        ViewStatsDto stat = getStatByEvent(event, false);

        UserShortDto initiator = userService.getUser(event.getInitiator());

        return EventMapper.toFullDto(event, initiator, stat.getHits());
    }

    @Loggable
    public EventDto getEventByIdByApi(long id) {
        Event event = service.getPublicEventById(id);

        if (event.getState() != EventState.PUBLISHED) {
           throw new ConflictException(String.format("Event %d not published.", id));
        }
        return EventMapper.mapToDto(event);
    }

    @Loggable
    public void updateConfirmedRequestsByEventId(Long eventId, String modify) {
        int i = switch (modify.toLowerCase()) {
            case "decrement" -> service.updateDecrementConfirmedRequests(eventId);
            case "increment" -> service.updateIncrementConfirmedRequests(eventId);
            default -> throw new ConflictException("Error: Parameter modify is incorrect. Value: " + modify);
        };

        if (i != 1) {
            throw new ProcessingException("Error: Sql entity doesn't update");
        }
    }

    @Loggable
    public EventDto updateConfirmedRequestsByOwner(EventDto eventToUpdate) {
        Event event = service.getPrivateEvent(eventToUpdate.getInitiator(), eventToUpdate.getId());

        Integer confirmedRequestsCount = eventToUpdate.getConfirmedRequests();

        if (event.getParticipantLimit() < confirmedRequestsCount) {
            throw new ConflictException("Error: Parameter count is bigger than events participant limit. Value: " + confirmedRequestsCount);
        }

        int confirmedOld = event.getConfirmedRequests();
        int limitOld = event.getParticipantLimit();

        event.setConfirmedRequests(confirmedOld + confirmedRequestsCount);
        event.setParticipantLimit(limitOld - confirmedRequestsCount);

        return EventMapper.mapToDto(
                service.saveEvent(event)
        );
    }

    private ViewStatsDto getStatByEvent(Event event, boolean uniq) {
        String uri = "/events/" + event.getId();

        List<ViewStatsDto> dtos = statClient.getStat(
                event.getCreated(),
                LocalDateTime.now(),
                List.of(uri),
                uniq);

        return dtos == null || dtos.isEmpty() ? new ViewStatsDto() : dtos.getFirst();
    }

    private Map<Long, Long> getStatsByUris(List<String> uris) {
        final LocalDateTime STATS_MIN =
                LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        final LocalDateTime STATS_MAX =
                LocalDateTime.of(9999, 12, 31, 23, 59, 59);

        List<ViewStatsDto> stats = statClient.getStat(STATS_MIN, STATS_MAX, uris, false);

        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> viewsMap = new LinkedHashMap<>();
        for (ViewStatsDto dto : stats) {
            String uri = dto.getUri();
            Long id = Long.parseLong(
                    uri.substring(
                            uri.lastIndexOf('/') + 1));
            viewsMap.put(id, dto.getHits());
        }

        return viewsMap;
    }

    private void sendStat(String uri, String address) {
        String serviceName = "ewm-event-service";

        statClient.hit(
                serviceName,
                uri,
                address,
                LocalDateTime.now()
        );
    }
}
