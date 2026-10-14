package ru.rostislav.cloudfilestorage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.rostislav.cloudfilestorage.dto.auth.UserRequest;
import ru.rostislav.cloudfilestorage.dto.auth.UserResponse;
import ru.rostislav.cloudfilestorage.service.AuthService;

@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody UserRequest userRequest
    ) {
        return authService.register(userRequest);
    }

}
