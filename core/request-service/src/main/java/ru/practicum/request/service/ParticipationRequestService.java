package ru.practicum.request.service;

import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;

import java.util.List;

public interface ParticipationRequestService {

    List<ParticipationRequestDto> getRequestsByEventId(long ownerId, long eventId);

    List<ParticipationRequestDto> getRequestsByUserId(Long userId);

    List<ParticipationRequest> getRequestsByEventId(Long eventId);

    ParticipationRequest saveRequest(ParticipationRequest request);

    void saveRequests(List<ParticipationRequest> updatedRequests);

    Boolean existsByEventIdAndRequesterId(Long userId, Long eventId);

    ParticipationRequest getRequestIfExistOrThrow(Long requestId);
}