package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dao.EventCustomRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.dao.LocationRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.exception.ConflictException;
import ru.practicum.event.exception.NotFoundException;
import ru.practicum.event.exception.ValidationException;
import ru.practicum.event.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);
    private final EventRepository eventRepository;
    private final EventCustomRepository eventCustomRepository;
    private final LocationRepository locationRepository;
    private final CategoryService categoryService;

    @Override
    @Transactional
    public Event addEvent(Long initiatorId, NewEventDto dto) {
        Category category = categoryService.getCategoryByIdOrElseThrow(dto.getCategory());
        Location location = getLocation(dto.getLocation().getLat(), dto.getLocation().getLon());
        LocalDateTime eventDate = getEventDateWithValidation(dto.getEventDate());

        Event newEvent = Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .eventDate(eventDate)
                .category(category)
                .initiator(initiatorId)
                .location(location)
                .confirmedRequests(0)
                .participantLimit(dto.getParticipantLimit())
                .paid(dto.getPaid())
                .requestModeration(dto.getRequestModeration())
                .created(LocalDateTime.now())
                .state(EventState.PENDING)
                .build();
        log.info("Запись в базу данных объекта Event: {}", newEvent);

        newEvent = eventRepository.save(newEvent);
        log.info("id объекта: {}", newEvent.getId());

        return newEvent;
    }

    @Override
    public Event getPrivateEvent(Long userId, Long eventId) {
        return getEventIfExistWithOwnerValidation(eventId, userId);
    }

    @Override
    @Transactional
    public Event updateEvent(Long userId, Long eventId,
                                    UpdateEventUserRequest request) {
        Event event = getEventIfExistWithOwnerValidation(eventId, userId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Event must not be published");
        }

        updateEventFieldsFromRequest(event, request);

        log.info("Запись в базу данных обновленного объекта Event: {}", event);
        return eventRepository.save(event);
    }

    @Override
    public List<Event> getPrivateEvents(long userId, int from, int size) {
        return eventCustomRepository.findUserEventsWithPagination(userId, from,  size);
    }

    @Override
    public List<Event> getPublicEvents(EventSearchParams params) {
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();

        if (rangeStart == null && rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeStart != null
                && rangeEnd != null
                && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        return eventCustomRepository.findPublicEventsWithPagination(params, rangeStart, rangeEnd);
    }

    @Override
    public Event getPublicEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));
    }

    @Override
    public List<Event> getEventsByAdmin(AdminEventSearchParams params) {
        boolean usersEmpty = isEmpty(params.getUsers());
        boolean statesEmpty = isEmpty(params.getStates());
        boolean categoriesEmpty = isEmpty(params.getCategories());

        List<Long> users = getIdsOrDefault(params.getUsers());
        List<String> states = getStatesOrDefault(params.getStates());
        List<Long> categories = getIdsOrDefault(params.getCategories());

        LocalDateTime rangeStart = getRangeStart(params.getRangeStart());
        LocalDateTime rangeEnd = getRangeEnd(params.getRangeEnd());

        return eventCustomRepository.findEventsByAdminFilters(
                params, users, states, categories,
                rangeStart, rangeEnd,
                usersEmpty, statesEmpty, categoriesEmpty
        );
    }

    @Override
    @Transactional
    public Event updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventIfExist(eventId);
        System.out.println("*******************************");
        System.out.println(event);

        if (request.getEventDate() != null) {
            LocalDateTime eventDate = parseDateTimeOrNull(request.getEventDate());
            validateAdminEventDate(eventDate);
            event.setEventDate(eventDate);
        }

        if (request.getCategory() != null) {
            Category category = categoryService.getCategoryByIdOrElseThrow(request.getCategory());
            event.setCategory(category);
        }

        if (request.getLocation() != null
                && request.getLocation().getLat() != null
                && request.getLocation().getLon() != null) {
            Location location = getLocation(request.getLocation().getLat(), request.getLocation().getLon());
            event.setLocation(location);
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getStateAction() != null) {
            updateAdminState(event, request.getStateAction());
        }

        log.info("Запись в базу данных обновленного администратором события: {}", event);
        return eventRepository.save(event);
    }

    @Override
    public List<Event> findAllByIdIn(Set<Long> ids) {
        return eventRepository.findAllByIdIn(ids);
    }

    @Override
    @Transactional
    public Integer updateDecrementConfirmedRequests(Long id) {
        return eventRepository.updateDecrementConfirmedRequests(id);
    }

    @Override
    @Transactional
    public Integer updateIncrementConfirmedRequests(Long id) {
        return eventRepository.updateIncrementConfirmedRequests(id);
    }

    @Override
    @Transactional
    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    private Event getEventIfExistWithOwnerValidation(long eventId, long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Field: eventId. Error: event не найден. Value: " + eventId));
        if (!event.getInitiator().equals(userId)) {
            throw new ConflictException("Field: userId. Error: Initiator has another id. Value: " + userId);
        }
        return event;
    }

    private LocalDateTime getEventDateWithValidation(String date) {
        LocalDateTime eventDate;
        if (!date.isBlank()) {
            eventDate = LocalDateTime.parse(date, FORMATTER);

            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Field: eventDate. Error: Начало события должно быть позже" +
                        LocalDateTime.now().plusHours(2) + ". Value: " + eventDate);
            }
        } else {
            eventDate = null;
        }
        return eventDate;
    }

    private EventState validateStateAction(String stateAction, EventState eventState) {
        switch (stateAction.trim().toUpperCase()) {
            case "SEND_TO_REVIEW" -> {
                if (eventState != EventState.CANCELED) {
                    throw new ConflictException("Only pending or canceled events can be changed");
                }
                return EventState.PENDING;
            }
            case "CANCEL_REVIEW" -> {
                if (eventState != EventState.PENDING) {
                    throw new ConflictException("Only pending or canceled events can be changed");
                }
                return EventState.CANCELED;
            }
            default -> throw new ValidationException("Field: stateAction. Error: must be SEND_TO_REVIEW or " +
                    "CANCEL_REVIEW. Value: " + stateAction);
        }
    }

    private boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private List<Long> getIdsOrDefault(List<Long> ids) {
        return isEmpty(ids) ? List.of(-1L) : ids;
    }

    private List<String> getStatesOrDefault(List<String> states) {
        if (isEmpty(states)) {
            return List.of(EventState.PENDING.name());
        }

        return states.stream()
                .map(state -> EventState.from(state.trim().toUpperCase()).name())
                .toList();
    }

    private LocalDateTime parseDateTimeOrNull(String date) {
        if (date == null) {
            return null;
        }

        if (date.isBlank()) {
            throw new ValidationException("Field: date. Error: date must not be blank");
        }

        try {
            return LocalDateTime.parse(date, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Field: date. Error: date must have format yyyy-MM-dd HH:mm:ss. Value: "
                    + date);
        }
    }

    private Event getEventIfExist(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Field: eventId. Error: event не найден. Value: " + eventId));
    }

    private void updateAdminState(Event event, String stateAction) {
        switch (stateAction.trim().toUpperCase()) {
            case "PUBLISH_EVENT" -> {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in the right state: "
                            + event.getState());
                }

                validateAdminEventDate(event.getEventDate());
                event.setState(EventState.PUBLISHED);
                event.setPublished(LocalDateTime.now());
            }
            case "REJECT_EVENT" -> {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject the event because it's already published");
                }

                event.setState(EventState.CANCELED);
            }
            default -> throw new ValidationException("Field: stateAction. Error: must be PUBLISH_EVENT or "
                    + "REJECT_EVENT. Value: " + stateAction);
        }
    }

    private void validateAdminEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Field: eventDate. Error: Начало события должно быть не ранее чем через час. "
                    + "Value: " + eventDate);
        }
    }

    private LocalDateTime getRangeStart(String rangeStart) {
        if (rangeStart == null) {
            return LocalDateTime.of(1900, 1, 1, 0, 0);
        }

        return parseDateTimeOrNull(rangeStart);
    }

    private LocalDateTime getRangeEnd(String rangeEnd) {
        if (rangeEnd == null) {
            return LocalDateTime.of(3000, 1, 1, 0, 0);
        }

        return parseDateTimeOrNull(rangeEnd);
    }

    private void updateEventFieldsFromRequest(Event event, UpdateEventUserRequest request) {
        if (request.getEventDate() != null) {
            LocalDateTime eventDate = getEventDateWithValidation(request.getEventDate());
            event.setEventDate(eventDate);
        }

        if (request.getCategory() != null) {
            Category category = categoryService.getCategoryByIdOrElseThrow(request.getCategory());
            event.setCategory(category);
        }

        if (request.getLocation() != null) {
            if (request.getLocation().getLat() != null && request.getLocation().getLon() != null) {
                Location location = getLocation(request.getLocation().getLat(), request.getLocation().getLon());
                event.setLocation(location);
            }
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getStateAction() != null) {
            EventState state = validateStateAction(request.getStateAction(), event.getState());
            event.setState(state);
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }


    private Location getLocation(Float Lat, Float Lon) {
        return locationRepository.findByLatAndLon(Lat, Lon)
                .orElseGet(() -> locationRepository.save(new Location(Lat, Lon)));
    }
}