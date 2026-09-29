package ru.rostislav.cloudfilestorage.security.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import ru.rostislav.cloudfilestorage.dto.ErrorResponse;
import ru.rostislav.cloudfilestorage.exception.auth.InvalidAuthenticationRequestException;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        HttpStatus status;

        String message;
        if (exception instanceof InvalidAuthenticationRequestException) {
            status = HttpStatus.BAD_REQUEST;
            message = exception.getMessage();
        } else if (exception instanceof BadCredentialsException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Invalid username or password";
        } else if (exception instanceof UsernameNotFoundException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Invalid username or password";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Oops... Something goes wrong";
        }

        ErrorResponse errorResponse = new ErrorResponse(message);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
