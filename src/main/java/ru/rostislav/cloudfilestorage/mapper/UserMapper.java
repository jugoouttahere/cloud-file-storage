package ru.rostislav.cloudfilestorage.mapper;

import ru.rostislav.cloudfilestorage.dto.auth.UserResponse;
import ru.rostislav.cloudfilestorage.entity.User;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getUsername());
    }
}
