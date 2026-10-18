package ru.practicum.user.service;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.dto.UsersGetParams;

import java.util.List;
import java.util.Set;

public interface UserService {

    UserShortDto getUserById(Long id);

    List<UserShortDto> getUsersByIds(Set<Long> ids);

    List<UserDto> getUsers(UsersGetParams params);

    UserDto addUser(NewUserRequest request);

    void deleteUser(Long userId);
}