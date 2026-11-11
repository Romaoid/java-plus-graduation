package ru.practicum.event.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.client.RecommendationsClient;
import ru.practicum.client.UserActionController;
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
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class EventOrchestrationService {
    private final EventService service;
    private final UserService userService;
    private final RequestService requestService;
    private final RecommendationsClient recommendationsClient;
    private final UserActionController userActionController;

    @Loggable
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        UserShortDto initiator = userService.getUser(userId);

        Event event = service.addEvent(initiator.getId(), dto);

        return EventMapper.toFullDto(event, initiator, 0.0);
    }

    @Loggable
    public EventFullDto getPrivateEvent(Long userId, Long eventId) {
        Event event = service.getPrivateEvent(userId, eventId);

        UserShortDto initiator = userService.getUser(userId);

        return EventMapper.toFullDto(event, initiator, getRatingById(eventId));
    }

    @Loggable
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = service.updateEvent(userId, eventId, request);

        UserShortDto initiator = userService.getUser(userId);

        return EventMapper.toFullDto(event, initiator, getRatingById(eventId));
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

        List<Long> ids = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Double> ratings = getRatingsByIds(ids);

        return events.stream()
                .map(view -> EventMapper.toShortDto(view, initiator, ratings.get(view.getId())))
                .toList();
    }

    @Loggable
    public List<EventShortDto> getPublicEvents(EventSearchParams params) {
        List<Event> events = service.getPublicEvents(params);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Double> ratings = getRatingsByIds(ids);

        Map<Long, UserShortDto> initiators = getInitiatorsByEventList(events);

        List<EventShortDto> result = events.stream()
                .map(view -> EventMapper.toShortDto(
                        view,
                        initiators.get(view.getInitiator()),
                        ratings.get(view.getId())
                ))
                .toList();

        if (params.getSort() == EventSort.RATING) {
            return result.stream()
                    .sorted(
                            Comparator.comparingDouble(EventShortDto::getRating)
                                    .reversed()
                    )
                    .toList();
        }

        return result;
    }

    @Loggable
    public EventFullDto getEventById(Long id, long userId) {
        Event event = service.getPublicEventById(id);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + id + " не найдено");
        }

        UserShortDto initiator = userService.getUser(event.getInitiator());

        if (userId != -1) {
            sendAction(id, userId, ActionTypeProto.ACTION_VIEW);
        }

        return EventMapper.toFullDto(event, initiator, getRatingById(id));
    }

    @Loggable
    public List<EventFullDto> getEventsByAdmin(AdminEventSearchParams params) {
        List<Event> events = service.getEventsByAdmin(params);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Double> ratings = getRatingsByIds(ids);

        Map<Long, UserShortDto> initiators = getInitiatorsByEventList(events);

        return events.stream()
                .map(view -> EventMapper.toFullDto(
                        view,
                        initiators.get(view.getInitiator()),
                        ratings.get(view.getId())))
                .toList();
    }

    @Loggable
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request){
        System.out.println(request);
        Event event = service.updateEventByAdmin(eventId, request);

        System.out.println(event);

        UserShortDto initiator = userService.getUser(event.getInitiator());

        return EventMapper.toFullDto(event, initiator, getRatingById(eventId));
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
    public List<EventDto> getEventsByIds(Set<Long> ids) {
        List<Event> events = service.findAllByIdIn(ids);

        if (events.isEmpty()) {
            throw new NotFoundException("Events not found.");
        }

        return events.stream().map(EventMapper::mapToDto).toList();
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
        int confirmedOld = event.getConfirmedRequests();

        if (event.getParticipantLimit() < confirmedOld + confirmedRequestsCount) {
            throw new ConflictException("Error: Parameter count is bigger than events participant limit. Value: " + confirmedRequestsCount);
        }

        event.setConfirmedRequests(confirmedOld + confirmedRequestsCount);

        return EventMapper.mapToDto(
                service.saveEvent(event)
        );
    }

    @Loggable
    public List<EventShortDto> getRecommendationsByUserId(Long userId, int maxEvents) {
        Map<Long, Double> top = topForUser(userId, maxEvents);
        if (top.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events = service.findAllByIdIn(top.keySet());

        Map<Long, UserShortDto> initiators = getInitiatorsByEventList(events);

        return events.stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        initiators.get(event.getInitiator()),
                        top.get(event.getId())
                ))
                .toList();
    }

    @Loggable
    public void sendLike(long eventId, long userId) {
        UserShortDto liker = userService.getUser(userId);

        Event event = service.getPublicEventById(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(String.format("Event %d not found", eventId));
        }

        if (event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new ValidationException("Error: Attend the event to evaluate. The event is due to begin");
        }

        ParticipationRequestDto request = requestService.getRequestStatus(liker.getId(), event.getId());

        if (request.getStatus() == ParticipationRequestDto.RequestStatus.NONREQUESTED) {
            throw new ValidationException(String.format("Error: Attend the event to evaluate. " +
                    "User %d did not submit a request to participate in event %d", userId, eventId));
        }

        if (request.getStatus() != ParticipationRequestDto.RequestStatus.CONFIRMED) {
            throw new ValidationException(String.format("Error: Attend the event to evaluate. " +
                    "Request's status by user %d is: %s", userId, request.getStatus().name().toLowerCase()));
        }

        sendAction(eventId, userId, ActionTypeProto.ACTION_LIKE);
    }

    @Loggable
    public List<EventShortDto> getSimilarEvents(long eventId, long userId, int maxResults) {
        Map<Long, Double> sim = similarForUser(eventId, userId, maxResults);
        if (sim.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events = service.findAllByIdIn(sim.keySet());

        Map<Long, UserShortDto> initiators = getInitiatorsByEventList(events);

        return events.stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        initiators.get(event.getInitiator()),
                        sim.get(event.getId())
                ))
                .toList();
    }

    private Double getRatingById(Long id) {
        List<RecommendedEventProto> scopeList = getRating(List.of(id));

        return scopeList.isEmpty() ? 0.0 : scopeList.getFirst().getScore();
    }

    private Map<Long, Double> getRatingsByIds(List<Long> ids) {
        List<RecommendedEventProto> scopeList = getRating(ids);

        Map<Long, Double> ratings = scopeList.stream().collect(Collectors.toMap(
                RecommendedEventProto::getEventId,
                RecommendedEventProto::getScore)
        );

        Map<Long, Double> result = new HashMap<>(ids.size());
        for (Long id : ids) {
            result.put(id, ratings.getOrDefault(id, 0.0));
        }

        return result;
    }

    private Map<Long, Double> topForUser(long userId, int max) {
        try (Stream<RecommendedEventProto> stream =
                     recommendationsClient.getRecommendationsForUser(userId, max)) {
            return stream.collect(Collectors.toMap(
                    RecommendedEventProto::getEventId,
                    RecommendedEventProto::getScore)
            );
        }
    }

    private Map<Long, Double> similarForUser(long eventId, long userId, int maxResults) {
        try (Stream<RecommendedEventProto> stream =
                     recommendationsClient.getSimilarEvents(eventId, userId, maxResults)) {
            return stream.collect(Collectors.toMap(
                    RecommendedEventProto::getEventId,
                    RecommendedEventProto::getScore)
            );
        }
    }

    private List<RecommendedEventProto> getRating(List<Long> eventIds) {
        try (Stream<RecommendedEventProto> stream = recommendationsClient.getInteractionsCount(eventIds)) {
            return stream.toList();
        }
    }

    private void sendAction(long eventId, long userId, ActionTypeProto action) {
        Instant now = Instant.now();

        userActionController.collectUserAction(UserActionProto
                .newBuilder()
                .setEventId(eventId)
                .setActionType(action)
                .setUserId(userId)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build()
        );
    }

    private Map<Long, UserShortDto> getInitiatorsByEventList(List<Event> events) {
        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiator)
                .collect(Collectors.toSet());

        return userService.getUsers(initiatorIds).stream()
                .collect(Collectors.toMap(
                        UserShortDto::getId,
                        userShortDto -> userShortDto
                ));
    }
}
