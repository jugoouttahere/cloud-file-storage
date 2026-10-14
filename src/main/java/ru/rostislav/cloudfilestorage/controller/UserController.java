package ru.rostislav.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.rostislav.cloudfilestorage.dto.auth.UserResponse;

@RequiredArgsConstructor
@RequestMapping("/api/user")
@RestController
public class UserController {

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse currentUser(Authentication authentication) {
        return new UserResponse(authentication.getName());
    }
}
