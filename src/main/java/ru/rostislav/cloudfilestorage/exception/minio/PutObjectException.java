package ru.rostislav.cloudfilestorage.exception.minio;

public class PutObjectException extends RuntimeException {
    public PutObjectException(String objectKey, Throwable cause) {
        super("Failed to put object: " + objectKey, cause);
    }
}
