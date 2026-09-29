package ru.rostislav.cloudfilestorage.mapper;

import org.springframework.stereotype.Component;
import ru.rostislav.cloudfilestorage.dto.auth.UserResponse;
import ru.rostislav.cloudfilestorage.entity.User;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getUsername());
    }
}
