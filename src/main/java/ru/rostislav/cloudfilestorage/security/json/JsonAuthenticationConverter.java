package ru.rostislav.cloudfilestorage.security.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import ru.rostislav.cloudfilestorage.dto.auth.UserRequest;
import ru.rostislav.cloudfilestorage.exception.auth.InvalidAuthenticationRequestException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class JsonAuthenticationConverter implements AuthenticationConverter {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Override
    public Authentication convert(HttpServletRequest request) {

        try {
            UserRequest userRequest = objectMapper.readValue(request.getInputStream(), UserRequest.class);

            Set<ConstraintViolation<UserRequest>> set = validator.validate(userRequest);
            if (!set.isEmpty()) {
                List<String> errorMessages = set.stream()
                        .map(ConstraintViolation::getMessage)
                        .toList();
                String joinedMessages = String.join("; ", errorMessages);
                throw new InvalidAuthenticationRequestException(joinedMessages);
            }

            return new UsernamePasswordAuthenticationToken(
                    userRequest.username(),
                    userRequest.password()
            );

        } catch (IOException e) {
            throw new InvalidAuthenticationRequestException("Invalid JSON", e);
        }
    }
}
