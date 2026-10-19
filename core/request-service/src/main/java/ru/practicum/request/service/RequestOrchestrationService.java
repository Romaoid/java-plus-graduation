package ru.practicum.request.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import ru.practicum.request.dto.EventDto;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.exception.ConflictException;
import ru.practicum.request.exception.NotFoundException;
import ru.practicum.request.exception.ProcessingException;
import ru.practicum.request.exception.ValidationException;
import ru.practicum.request.feign.EventClient;
import ru.practicum.request.logger.Loggable;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.model.RequestUpdateModifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestOrchestrationService {
    private final ParticipationRequestService requestService;
    private final EventClient eventClient;

    @Loggable
    public ParticipationRequestDto sendRequest(Long userId, Long eventId) {
        if (isRequestAlreadyExist(userId, eventId)) {
            throw new ConflictException("The request to event id: " + eventId +
                    " from user id: " + userId + "is already exists.");
        }

        EventDto event = getEventById(eventId);
        if (event.getInitiator().equals(userId)) {
            throw new ConflictException("The request to own event is rejected");
        }

        int limit = event.getParticipantLimit();
        int confirmedRequests = event.getConfirmedRequests();
        if (limit == confirmedRequests && limit != 0) {
            throw new ConflictException("The participant limit has been reached to event id: " + eventId);
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setEventId(eventId);
        request.setRequesterId(userId);
        if (!event.getRequestModeration() || limit == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            updateConfirmedRequests(event.getId(), RequestUpdateModifier.INCREMENT);
        }

        log.info("Save to RequestRepository entity: [EventId:{}, RequesterId:{}, Status: {}, Created: {}",
                request.getEventId(),
                request.getRequesterId(),
                request.getStatus(),
                request.getCreated());

        try {
            request = requestService.saveRequest(request);
        } catch (Exception e) {
            log.error(e.getMessage());
            updateConfirmedRequests(event.getId(), RequestUpdateModifier.DECREMENT);
            throw new ProcessingException("Request-service stop requesting with error, please try later.");
        }

        log.info("request saved with id: {}", request.getId());

        return RequestMapper.toDto(request);
    }

    @Loggable
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestService.getRequestIfExistOrThrow(requestId);

        if (!request.getRequesterId().equals(userId)) {
            throw new NotFoundException("Request with id= " + requestId + " was not found");
        }

        RequestStatus oldStatus = request.getStatus();
        try {
            request.setStatus(RequestStatus.CANCELED);

            requestService.saveRequest(request);

            if (request.getStatus() == RequestStatus.CONFIRMED) {
                updateConfirmedRequests(requestId, RequestUpdateModifier.DECREMENT);
            }
        } catch (DataAccessResourceFailureException e) {
            log.error(e.getMessage());
            throw new ProcessingException("Request-service stop requesting with error, please try later.");
        } catch (ProcessingException e) {
            log.error(e.getMessage());
            request.setStatus(oldStatus);
            requestService.saveRequest(request);
            throw new ProcessingException("Request-service stop requesting with error, please try later.");
        }

        return RequestMapper.toDto(request);
    }

    @Loggable
    public List<ParticipationRequestDto> getRequestsByUserId(Long id) {
        return requestService.getRequestsByUserId(id);
    }

    @Loggable
    public List<ParticipationRequestDto> getRequestsByEventId(long ownerId, long eventId) {
        getEventIfExistWithOwnerValidation(ownerId, eventId);

        return requestService.getRequestsByEventId(ownerId, eventId);
    }

    @Loggable
    public EventRequestStatusUpdateResult updateOwnParticipationRequests(Long ownerId, Long eventId,
                                                                         EventRequestStatusUpdateRequest request) {
        EventDto event = getEventIfExistWithOwnerValidation(ownerId, eventId);

        long limit = event.getParticipantLimit();
        if (limit == 0) {
            throw new ConflictException("The participant limit is 0, moderation is disabled");
        }
        if (!event.getRequestModeration()) {
            throw new ConflictException("The request moderation is disabled");
        }

        List<ParticipationRequest> requests = findRequestsByEventId(eventId);
        //проверяем, что лимит для заявок не исчерпан
        long countConfirmed = requests.stream()
                .filter(req -> req.getStatus() == RequestStatus.CONFIRMED)
                .count();

        if (countConfirmed >= limit) {
            throw new ConflictException("The participant limit has been reached");
        }

        //проверяем, что заявки в состоянии Ожидания
        Set<Long> idsToCheck = new HashSet<>(request.getRequestIds());

        boolean allPending = requests.stream()
                .filter(req -> idsToCheck.contains(req.getId()))
                .allMatch(req -> req.getStatus() == RequestStatus.PENDING);

        if (!allPending) {
            throw new ValidationException("Request must have status PENDING");
        }

        List<ParticipationRequest> updatedRequests = new ArrayList<>();
        List<ParticipationRequest> toConfirm = new ArrayList<>();
        List<ParticipationRequest> toReject;

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            long availableSlots = limit - countConfirmed;

            List<ParticipationRequest> targetRequests = requests.stream()
                    .filter(req -> request.getRequestIds().contains(req.getId()))
                    .toList();

            toConfirm = targetRequests.stream()
                    .limit(availableSlots)
                    .toList();
            toReject = targetRequests.stream()
                    .skip(availableSlots)
                    .toList();

            toConfirm.forEach(req -> req.setStatus(RequestStatus.CONFIRMED));
            toReject.forEach(req -> req.setStatus(RequestStatus.REJECTED));

        } else if (request.getStatus() == RequestStatus.REJECTED) {
            // Все запрашиваемые заявки отклоняем
            toReject = requests.stream()
                    .filter(req -> request.getRequestIds().contains(req.getId()))
                    .toList();
            toReject.forEach(req -> req.setStatus(RequestStatus.REJECTED));

        } else {
            throw new ValidationException("Field: status. Error: must be CONFIRMED or REJECTED. Value: " +
                    request.getStatus());
        }

        updatedRequests.addAll(toConfirm);
        updatedRequests.addAll(toReject);

        int newConfirmedRequests = toConfirm.size();
        if (newConfirmedRequests > 0) {
            event.setConfirmedRequests(newConfirmedRequests + event.getConfirmedRequests());
            updateConfirmedRequestsByOwner(event);
        }

        for (ParticipationRequest req : updatedRequests) {
            log.info("Save to RequestRepository updated entity: [EventId:{}, RequesterId:{}, Status: {}, Created: {}",
                    req.getEventId(),
                    req.getRequesterId(),
                    req.getStatus(),
                    req.getCreated());
        }
        requestService.saveRequests(updatedRequests);

        //формирование ответа
        List<ParticipationRequestDto> confirmed = toConfirm.stream().map(RequestMapper::toDto).toList();
        List<ParticipationRequestDto> rejected = toReject.stream().map(RequestMapper::toDto).toList();

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    private EventDto getEventById(long id) {
        try {
            return eventClient.getEventById(id);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Field: eventId. Error: event не найден. Value: " + id);
        } catch (FeignException.Conflict e) {
            throw new ConflictException("Field: eventId. Error: event не найден. Value: " + id);
        } catch (FeignException.ServiceUnavailable e) {
            throw new ProcessingException("Event service is unavailable now, please try later.");
        }
    }

    @Loggable
    private void updateConfirmedRequests(long eventId, RequestUpdateModifier modify) {
            eventClient.updateConfirmedRequestsFromEventById(eventId, modify.name());
    }

    @Loggable
    private EventDto updateConfirmedRequestsByOwner(EventDto event) {
            return eventClient.updateConfirmedRequestsFromEventByIdByOwner(event);
    }

    private Boolean isRequestAlreadyExist(Long userId, Long eventId) {
        return requestService.existsByEventIdAndRequesterId(userId, eventId);
    }

    private EventDto getEventIfExistWithOwnerValidation(long ownerId, long eventId) {
        EventDto event;
        try {
            event = getEventById(eventId);
        } catch (ConflictException ignored) {
            throw new ConflictException("Field: event. Error: Event is no published.");
        }

        if (event.getInitiator() != ownerId) {
            throw new ConflictException("Field: userId. Error: Initiator has another id. Value: " + ownerId);
        }
        return event;
    }

    private List<ParticipationRequest> findRequestsByEventId(Long eventId) {
        return  requestService.getRequestsByEventId(eventId);
    }
}
