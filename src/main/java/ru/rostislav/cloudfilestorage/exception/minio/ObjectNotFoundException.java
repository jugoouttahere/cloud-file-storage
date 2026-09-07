package ru.rostislav.cloudfilestorage.exception.minio;

public class ObjectNotFoundException extends RuntimeException {
    public ObjectNotFoundException(String objectKey) {
        super(String.format("Resource not found: %s", objectKey));
    }
}
