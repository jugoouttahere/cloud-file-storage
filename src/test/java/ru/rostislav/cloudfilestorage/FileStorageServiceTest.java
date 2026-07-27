package ru.rostislav.cloudfilestorage;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.rostislav.cloudfilestorage.service.FileStorageService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

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