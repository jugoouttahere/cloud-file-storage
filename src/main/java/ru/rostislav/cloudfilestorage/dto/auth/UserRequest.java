package ru.rostislav.cloudfilestorage.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Username cant be empty")
        @Size(min = 2, max = 50)
        String username,
        @NotBlank
        @Size(min = 8, max = 50)
        String password) {
}
