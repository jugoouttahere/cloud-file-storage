package ru.rostislav.cloudfilestorage.exception.minio;

public class ObjectAlreadyExistsException extends RuntimeException {
    public ObjectAlreadyExistsException(String objectKey) {
        super(String.format("Resource already exists: %s", objectKey));
    }
}
