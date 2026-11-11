package ru.practicum.event.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.event.dto.ParticipationRequestDto;
import ru.practicum.event.exception.ProcessingException;
import ru.practicum.event.feign.RequestClient;
import ru.practicum.event.logger.Loggable;

@Component
@RequiredArgsConstructor
public class RequestService {
    private final RequestClient client;

    @Loggable
    public ParticipationRequestDto getRequestStatus(long userId, long eventId) {
        try {
            return client.getRequestStatus(userId, eventId);
        } catch (FeignException.ServiceUnavailable exception) {
            throw new ProcessingException("Сервис request временно недоступен");
        }
    }
}
