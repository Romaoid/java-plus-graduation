package ru.practicum.event.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.event.dto.UserShortDto;

import java.util.List;
import java.util.Set;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserShortDto getUserById(@PathVariable("id") Long id);

    @PostMapping("/api/users")
    List<UserShortDto> getUsersByIds(@RequestBody Set<Long> ids);
}
