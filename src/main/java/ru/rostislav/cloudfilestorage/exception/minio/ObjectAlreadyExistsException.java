package ru.rostislav.cloudfilestorage.exception.minio;

public class ObjectAlreadyExistsException extends RuntimeException {
    public ObjectAlreadyExistsException(String objectKey) {
        super(String.format("File with name:%s already exist.", objectKey));
    }
}
