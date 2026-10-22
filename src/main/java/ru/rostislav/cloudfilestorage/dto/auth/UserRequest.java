package ru.rostislav.cloudfilestorage.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Username cant be empty")
        @Size(min = 2, max = 50, message = "Username must be 2 - 50 symbols")
        String username,
        @NotBlank(message = "Password cant be empty")
        @Size(min = 8, max = 50, message = "Password must be 8 - 50 symbols")
        String password
) {
}
