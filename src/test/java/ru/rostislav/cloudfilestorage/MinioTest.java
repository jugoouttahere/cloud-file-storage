package ru.rostislav.cloudfilestorage;

import io.minio.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MinioTest extends IntegrationTest {

    @Autowired
    private MinioClient minioClient;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        if (!minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket("cloud-storage")
                .build())) {

            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket("cloud-storage")
                            .build()
            );
        }
    }

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

        ObjectWriteResponse objectWriteResponse = minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("hello.txt")
                        .stream(inputStream, bytes.length, -1)
                        .contentType("text/plain")
                        .build()
        );

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
