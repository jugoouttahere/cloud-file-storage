package ru.rostislav.cloudfilestorage.exception.minio;

public class ObjectNotFoundException extends RuntimeException {
    public ObjectNotFoundException(String objectKey) {
        super(String.format("File with name:%s not found.", objectKey));
    }
}
