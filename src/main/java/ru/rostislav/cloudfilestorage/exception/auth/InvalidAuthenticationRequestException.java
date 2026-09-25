package ru.rostislav.cloudfilestorage.exception.auth;

import org.springframework.security.core.AuthenticationException;

public class InvalidAuthenticationRequestException extends AuthenticationException {
    public InvalidAuthenticationRequestException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public InvalidAuthenticationRequestException(String msg) {
        super(msg);
    }
}
