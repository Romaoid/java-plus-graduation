package ru.practicum.event.service;

import com.github.benmanes.caffeine.cache.Cache;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;
import ru.practicum.event.dto.UserShortDto;
import ru.practicum.event.exception.NotFoundException;
import ru.practicum.event.exception.ProcessingException;
import ru.practicum.event.feign.UserClient;
import ru.practicum.event.logger.Loggable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserService {
    private final UserClient userClient;
    private final CacheManager cacheManager;

    @Loggable
    @Cacheable(value = "users", key = "#userId")
    public UserShortDto getUser(Long userId) {
       try {
            return userClient.getUserById(userId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("User id:%d not found".formatted(userId));
        } catch (FeignException.ServiceUnavailable exception) {
            throw new ProcessingException("Сервис users временно недоступен");
        }
    }

    @Loggable
    public List<UserShortDto> getUsers(Set<Long> ids) {
        CaffeineCache springCache = (CaffeineCache) cacheManager.getCache("users");

        if (springCache == null) {
            return getUsersFromFeign(ids);
        }

        Cache<Object, Object> nativeCache = springCache.getNativeCache();
        Map<Object, Object> cachedMap = nativeCache.getAllPresent(ids);

        Set<Long> missingIds = ids.stream()
                .filter(id -> !cachedMap.containsKey(id))
                .collect(Collectors.toSet());

        List<UserShortDto> result = new ArrayList<>();
        cachedMap.values().forEach(v -> result.add((UserShortDto) v));

        if (!missingIds.isEmpty()) {
            List<UserShortDto> fetched = getUsersFromFeign(missingIds);
            result.addAll(fetched);
            Map<Object, Object> toPut = fetched.stream()
                    .collect(Collectors.toMap(
                            UserShortDto::getId,
                            UserShortDto -> UserShortDto));
            nativeCache.putAll(toPut);
        }

        return result;
    }

    private List<UserShortDto> getUsersFromFeign(Set<Long> ids) {
        try {
            return userClient.getUsersByIds(ids);
        } catch (FeignException.ServiceUnavailable exception) {
            throw new ProcessingException("Сервис users временно недоступен");
        }
    }
}
