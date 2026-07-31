package ru.rostislav.cloudfilestorage;

import io.minio.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import ru.rostislav.cloudfilestorage.exception.minio.EmptyFileException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectAlreadyExistsException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectNotFoundException;
import ru.rostislav.cloudfilestorage.service.FileStorageService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceTest extends MinioIntegrationTest {

    @Autowired
    private FileStorageService fileStorageService;

    @SneakyThrows
    @Test
    void shouldRenameFile() {
        String text = "Hello MinIO";

        putObject("hello.txt", text);

        fileStorageService.renameFile("hello.txt", "new-hello.txt");

        GetObjectResponse getObjectResponse = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("new-hello.txt")
                        .build()
        );

        String actual = new String(getObjectResponse.readAllBytes(), StandardCharsets.UTF_8);

        assertFalse(isObjectExist("hello.txt"));
        assertTrue(isObjectExist("new-hello.txt"));
        assertEquals(text, actual);
    }

    @SneakyThrows
    @Test
    void shouldRenameFolder() {
        putObject("old/file1.txt", "");
        putObject("old/file2.txt", "");
        putObject("old/inner/file3.txt", "");

        fileStorageService.renameFolder("old/", "new/");

        assertTrue(isObjectExist("new/file1.txt"));
        assertTrue(isObjectExist("new/file2.txt"));
        assertTrue(isObjectExist("new/inner/file3.txt"));
        assertFalse(isObjectExist("old/file1.txt"));
        assertFalse(isObjectExist("old/file2.txt"));
        assertFalse(isObjectExist("old/inner/file3.txt"));
    }

    @SneakyThrows
    @Test
    void shouldDeleteFile() {
        putObject("hello.txt", "");

        fileStorageService.deleteFile("hello.txt");

        assertFalse(isObjectExist("hello.txt"));
    }

    @SneakyThrows
    @Test
    void shouldUploadFile() {
        String expected = "Hello MinIO";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                expected.getBytes(StandardCharsets.UTF_8)
        );

        fileStorageService.uploadFile("hello.txt", file);

        GetObjectResponse getObjectResponse = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("hello.txt")
                        .build()
        );

        String actual = new String(getObjectResponse.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(isObjectExist("hello.txt"));
        assertEquals(expected, actual);
    }

    @SneakyThrows
    @Test
    void shouldDownloadFile() {
        String expected = "Hello MinIO";

        putObject("hello.txt", expected);

        String actual;
        try (InputStream downloadFile = fileStorageService.downloadFile("hello.txt")) {
            actual = new String(downloadFile.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(expected, actual);
    }

    @SneakyThrows
    @Test
    void shouldCreateEmptyFolder() {
        fileStorageService.createEmptyFolder("folder/");

        StatObjectResponse statObject = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("folder/")
                        .build()
        );

        assertEquals(0, statObject.size());
    }

    @SneakyThrows
    @Test
    void shouldThrowWhenUploadingExistingFile() {
        putObject("hello.txt", "");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                "Hello".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(
                ObjectAlreadyExistsException.class,
                () -> fileStorageService.uploadFile("hello.txt", file)
        );
    }

    @SneakyThrows
    @Test
    void shouldThrowWhenUploadingEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                new byte[0]
        );

        assertThrows(
                EmptyFileException.class,
                () -> fileStorageService.uploadFile("hello.txt", file)
        );
    }

    @SneakyThrows
    @Test
    void shouldThrowWhenDownloadingMissingFile() {
        assertThrows(
                ObjectNotFoundException.class,
                () -> fileStorageService.downloadFile("hello.txt")
        );
    }

    @Test
    void shouldThrowWhenDeletingMissingFile() {
        assertThrows(
                ObjectNotFoundException.class,
                () -> fileStorageService.deleteFile("hello.txt")
        );
    }

    @Test
    void shouldThrowWhenRenamingMissingFile() {
        assertThrows(
                ObjectNotFoundException.class,
                () -> fileStorageService.renameFile("hello.txt", "new-hello.txt")
        );
    }

    @SneakyThrows
    @Test
    void shouldThrowWhenRenamingToExistingFile() {
        String text = "Hello MinIO";

        putObject("hello.txt", text);

        assertThrows(
                ObjectAlreadyExistsException.class,
                () -> fileStorageService.renameFile("hello.txt", "hello.txt")
        );
    }

    @SneakyThrows
    @Test
    void shouldThrowWhenCreatingExistingFolder() {
        putObject("folder/", "");

        assertThrows(
                ObjectAlreadyExistsException.class,
                () -> fileStorageService.createEmptyFolder("folder/")
        );
    }
}