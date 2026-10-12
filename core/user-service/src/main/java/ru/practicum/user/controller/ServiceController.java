package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user.model.User;
import ru.practicum.user.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ServiceController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public User getUserById(@PathVariable Long userId) {
        log.info("get /users/{}", userId);
        return userService.getUserById(userId);
    }
}
