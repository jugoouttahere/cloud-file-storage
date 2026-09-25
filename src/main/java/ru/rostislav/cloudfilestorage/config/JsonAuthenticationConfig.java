package ru.rostislav.cloudfilestorage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import ru.rostislav.cloudfilestorage.security.json.JsonAuthenticationConverter;
import ru.rostislav.cloudfilestorage.security.json.JsonAuthenticationFailureHandler;
import ru.rostislav.cloudfilestorage.security.json.JsonAuthenticationFilter;
import ru.rostislav.cloudfilestorage.security.json.JsonAuthenticationSuccessHandler;

@RequiredArgsConstructor
@Configuration
public class JsonAuthenticationConfig {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final JsonAuthenticationSuccessHandler successHandler;
    private final JsonAuthenticationFailureHandler failureHandler;
    private final SecurityContextRepository securityContextRepository;

    @Bean
    public JsonAuthenticationFilter jsonAuthenticationFilter(
            AuthenticationManager authenticationManager
    ) {
        JsonAuthenticationConverter converter =
                new JsonAuthenticationConverter(objectMapper, validator);

        RequestMatcher matcher =
                PathPatternRequestMatcher.withDefaults()
                        .matcher(HttpMethod.POST, "/api/auth/sign-in");

        JsonAuthenticationFilter filter =
                new JsonAuthenticationFilter(
                        authenticationManager,
                        converter,
                        matcher
                );

        filter.setSuccessHandler(successHandler);
        filter.setFailureHandler(failureHandler);
        filter.setSecurityContextRepository(securityContextRepository);

        return filter;
    }
}
