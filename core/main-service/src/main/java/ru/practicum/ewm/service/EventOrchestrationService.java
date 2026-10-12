package ru.practicum.ewm.service;

import feign.FeignException;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.NewEventDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.feign.UserClient;
import ru.practicum.ewm.model.User;

@Service
@RequiredArgsConstructor
public class EventOrchestrationService {
    private final EventService service;
    private final UserClient userClient;

    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        try {
            User initiator = userClient.getUserById(userId);

            return service.addEventForTestFeign(initiator, dto);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("User id:%d not found".formatted(userId));
        } catch (FeignException.ServiceUnavailable exception) {
            throw new InternalServerErrorException("Сервис users временно недоступен");
        }
    }
}
