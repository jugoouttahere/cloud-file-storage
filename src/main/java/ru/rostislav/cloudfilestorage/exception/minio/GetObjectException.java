package ru.rostislav.cloudfilestorage.exception.minio;

public class GetObjectException extends RuntimeException {
    public GetObjectException(String objectKey, Throwable cause) {
        super("Failed to get object: " + objectKey, cause);
    }
}
