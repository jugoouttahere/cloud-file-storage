package ru.rostislav.cloudfilestorage.exception;

public class InvalidPathException extends RuntimeException {
    public InvalidPathException(String path) {
        super(String.format("Invalid path: %s", path));
    }
}
