package ru.rostislav.cloudfilestorage.exception.minio;

public class ObjectOperationException extends RuntimeException {
    public ObjectOperationException(String objectKey, String operation, Throwable cause) {
        super(String.format("Failed to %s object: %s", operation, objectKey), cause);
    }
}
