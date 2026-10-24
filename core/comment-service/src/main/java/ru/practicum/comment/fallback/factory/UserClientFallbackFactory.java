package ru.practicum.comment.fallback.factory;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.comment.dto.UserShortDto;
import ru.practicum.comment.exception.ProcessingException;
import ru.practicum.comment.feign.UserClient;

import java.util.List;
import java.util.Set;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public UserShortDto getUserById(Long id) {
                if (cause instanceof FeignException feignException
                        && feignException.status() >= 400 && feignException.status() < 500) {
                    throw feignException;
                }
                throw  new ProcessingException("User service unavailable");
            }

            @Override
            public List<UserShortDto> getUsersByIds(Set<Long> ids) {
                return ids.stream()
                        .map(id -> UserShortDto.builder().id(id).name("User" + id).build())
                        .toList();
            }
        };
    }
}
