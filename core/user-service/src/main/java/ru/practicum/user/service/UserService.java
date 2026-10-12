package ru.practicum.user.service;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UsersGetParams;
import ru.practicum.user.model.User;

import java.util.List;

public interface UserService {

    User getUserById(Long id);

    List<UserDto> getUsers(UsersGetParams params);

    UserDto addUser(NewUserRequest request);

    void deleteUser(Long userId);
}