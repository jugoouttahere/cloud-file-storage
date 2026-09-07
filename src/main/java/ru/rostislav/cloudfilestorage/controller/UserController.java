package ru.rostislav.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rostislav.cloudfilestorage.dto.auth.UserResponse;
import ru.rostislav.cloudfilestorage.service.UserService;

@RequiredArgsConstructor
@RequestMapping("/api/user")
@RestController
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser(Authentication authentication) {
        UserResponse currentUser = userService.getCurrentUser(authentication);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(currentUser);
    }
}
