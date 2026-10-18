package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.service.UserService;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ServiceController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public UserShortDto getUserById(@PathVariable Long userId) {
        log.info("GET /users/{}", userId);
        return userService.getUserById(userId);
    }

    @PostMapping
    public List<UserShortDto> getUsersByIds(@RequestBody Set<Long> ids) {
        log.info("POST /users with body: {}", ids);
        return userService.getUsersByIds(ids);
    }
}
