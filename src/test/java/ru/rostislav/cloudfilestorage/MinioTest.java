package ru.rostislav.cloudfilestorage;

import io.minio.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import ru.rostislav.cloudfilestorage.integration.MinioIntegrationTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MinioTest extends MinioIntegrationTest {

    @SneakyThrows
    @Test
    void shouldReturnTrueWhenBucketExists() {
        assertTrue(minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket("cloud-storage")
                .build()));
    }

    @SneakyThrows
    @Test
    void shouldCreateFile() {
        String expected = "Hello MinIO";
        byte[] bytes = expected.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        putObject("hello.txt", expected);

        GetObjectResponse getObjectResponse = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("hello.txt")
                        .build()
        );

        String actual = new String(getObjectResponse.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(expected, actual);
    }
}
