package ru.rostislav.cloudfilestorage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import ru.rostislav.cloudfilestorage.dto.auth.UserRequest;
import ru.rostislav.cloudfilestorage.dto.auth.UserResponse;
import ru.rostislav.cloudfilestorage.service.AuthService;

@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/sign-up")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest userRequest) {
        UserResponse registeredUser = authService.register(userRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(registeredUser);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody UserRequest userRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Authentication authentication = authService.authenticate(userRequest);

        httpRequest.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        UserResponse loggedUser = new UserResponse(authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(loggedUser);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Authentication authentication
    ) {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

        logoutHandler.logout(httpRequest, httpResponse, authentication);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
