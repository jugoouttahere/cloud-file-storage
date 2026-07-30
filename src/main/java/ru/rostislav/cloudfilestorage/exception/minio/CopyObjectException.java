package ru.rostislav.cloudfilestorage.exception.minio;

public class CopyObjectException extends RuntimeException {
    public CopyObjectException(String objectKey, Throwable cause) {
        super("Failed to copy object: " + objectKey, cause);
    }
}
