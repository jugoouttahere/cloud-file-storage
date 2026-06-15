package ru.rostislav.cloudfilestorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.rostislav.cloudfilestorage.dto.auth.UserRequest;
import ru.rostislav.cloudfilestorage.dto.auth.UserResponse;
import ru.rostislav.cloudfilestorage.entity.User;
import ru.rostislav.cloudfilestorage.mapper.UserMapper;
import ru.rostislav.cloudfilestorage.repository.UserRepository;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    public UserResponse register(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("User already exist");
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.username(), encodedPassword);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    public UserResponse login(UserRequest request) {
        return null;
    }

    public void logout() {

    }
}
