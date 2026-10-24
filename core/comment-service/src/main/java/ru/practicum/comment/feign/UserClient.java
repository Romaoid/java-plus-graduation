package ru.practicum.comment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.UserShortDto;
import ru.practicum.comment.fallback.factory.UserClientFallbackFactory;

import java.util.List;
import java.util.Set;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserShortDto getUserById(@PathVariable("id") Long id);

    @PostMapping("/api/users")
    List<UserShortDto> getUsersByIds(@RequestBody Set<Long> ids);
}
