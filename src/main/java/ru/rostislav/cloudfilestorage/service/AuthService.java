package ru.rostislav.cloudfilestorage.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import ru.rostislav.cloudfilestorage.dto.auth.UserRequest;
import ru.rostislav.cloudfilestorage.dto.auth.UserResponse;
import ru.rostislav.cloudfilestorage.entity.User;
import ru.rostislav.cloudfilestorage.exception.InvalidCredentialsException;
import ru.rostislav.cloudfilestorage.exception.UserAlreadyExistsException;
import ru.rostislav.cloudfilestorage.mapper.UserMapper;
import ru.rostislav.cloudfilestorage.repository.UserRepository;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public UserResponse register(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException(request.username());
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.username(), encodedPassword);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    public UserResponse login(UserRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(context, httpServletRequest, httpServletResponse);

            return new UserResponse(authentication.getName());
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }
    }

    public void logout() {

    }
}
