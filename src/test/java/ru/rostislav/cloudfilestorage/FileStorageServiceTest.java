package ru.rostislav.cloudfilestorage;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import ru.rostislav.cloudfilestorage.service.FileStorageService;
import ru.rostislav.cloudfilestorage.service.MinioService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceTest extends IntegrationTest {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private FileStorageService fileStorageService;

    @SneakyThrows
    @Test
    void shouldRenameFile() {
        String text = "Hello MinIO";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("hello.txt")
                        .stream(inputStream, bytes.length, -1)
                        .contentType("text/plain")
                        .build()
        );

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
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("old/file1.txt")
                        .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                        .build()
        );
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("old/file2.txt")
                        .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                        .build()
        );
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("old/inner/file3.txt")
                        .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                        .build()
        );

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
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("hello.txt")
                        .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                        .build()
        );

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

        fileStorageService.uploadFile(file, "hello.txt");

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
        byte[] bytes = expected.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("hello.txt")
                        .stream(inputStream, bytes.length, -1)
                        .contentType("text/plain")
                        .build()
        );

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
    private boolean isObjectExist(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket("cloud-storage")
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new RuntimeException("Failed to check object existence", e);
        }
    }
}