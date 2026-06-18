package ru.rostislav.cloudfilestorage.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super("Username already exists: " + message);
    }
}
