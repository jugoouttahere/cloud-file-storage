package ru.rostislav.cloudfilestorage.exception.minio;

public class RemoveObjectException extends RuntimeException {
    public RemoveObjectException(String objectKey, Throwable cause) {
        super("Failed to remove object: " + objectKey, cause);
    }
}
