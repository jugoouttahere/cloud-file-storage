package ru.rostislav.cloudfilestorage.exception.minio;

public class EmptyFileException extends RuntimeException {
    public EmptyFileException(String fileName) {
        super("File is empty: " + fileName);
    }
}
