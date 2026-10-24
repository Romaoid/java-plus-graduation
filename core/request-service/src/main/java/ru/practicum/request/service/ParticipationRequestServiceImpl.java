package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.request.dao.ParticipationRequestRepository;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.exception.NotFoundException;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private static final Logger log = LoggerFactory.getLogger(ParticipationRequestServiceImpl.class);
    private final ParticipationRequestRepository requestRepository;

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(long ownerId, long eventId) {
        List<ParticipationRequest> requests =
                requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);

        return requests.stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    public List<ParticipationRequest> getRequestsByEventId(Long eventId) {
        return requestRepository.findAllByEventId(eventId);
    }

    @Override
    @Transactional
    public void saveRequests(List<ParticipationRequest> updatedRequests) {
        requestRepository.saveAll(updatedRequests);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByUserId(Long userId) {
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequest saveRequest(ParticipationRequest request) {
        return requestRepository.save(request);
    }

    @Override
    public Boolean existsByEventIdAndRequesterId(Long userId, Long eventId) {
        return requestRepository.existsByEventIdAndRequesterId(eventId, userId);
    }

    @Override
    public ParticipationRequest getRequestIfExistOrThrow(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id= " + requestId + " was not found"));
    }
}