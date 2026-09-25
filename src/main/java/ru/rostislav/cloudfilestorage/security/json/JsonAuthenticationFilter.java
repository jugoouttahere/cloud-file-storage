package ru.rostislav.cloudfilestorage.security.json;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class JsonAuthenticationFilter extends AuthenticationFilter {
    public JsonAuthenticationFilter(AuthenticationManager authenticationManager, AuthenticationConverter authenticationConverter, RequestMatcher requestMatcher) {
        super(authenticationManager, authenticationConverter);
        setRequestMatcher(requestMatcher);
    }
}
