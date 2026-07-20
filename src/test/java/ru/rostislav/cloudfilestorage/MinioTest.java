package ru.rostislav.cloudfilestorage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
}
