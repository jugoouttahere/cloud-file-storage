package ru.rostislav.cloudfilestorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import ru.rostislav.cloudfilestorage.dto.auth.UserResponse;

@RequiredArgsConstructor
@Service
public class UserService {

    public UserResponse getCurrentUser(Authentication authentication) {
        return new UserResponse(
                authentication.getName()
        );
    }
}
