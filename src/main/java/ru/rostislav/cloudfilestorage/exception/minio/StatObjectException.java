package ru.rostislav.cloudfilestorage.exception.minio;

public class StatObjectException extends RuntimeException {
    public StatObjectException(String objectKey, Throwable cause) {
        super("Failed to get object stat: " + objectKey, cause);
    }
}
