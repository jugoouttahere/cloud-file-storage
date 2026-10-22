package ru.rostislav.cloudfilestorage.integration;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectOperationException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class MinioIntegrationTest extends IntegrationTest {

    @Autowired
    protected MinioClient minioClient;

    @BeforeEach
    void cleanBucket() throws Exception {
        for (Result<Item> result : minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("user-files")
                        .recursive(true)
                        .build())) {

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("user-files")
                            .object(result.get().objectName())
                            .build()
            );
        }
    }

    @SneakyThrows
    protected void putObject(String objectKey, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("user-files")
                        .object("user-1-files/" + objectKey)
                        .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                        .contentType("text/plain")
                        .build()
        );
    }

    @SneakyThrows
    protected void putFolder(String folderKey) {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("user-files")
                        .object("user-1-files/" + folderKey)
                        .stream(
                                new ByteArrayInputStream(new byte[0]),
                                0,
                                -1
                        )
                        .build()
        );
    }

    @SneakyThrows
    protected boolean isObjectExist(String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket("user-files")
                            .object("user-1-files/" + objectKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new ObjectOperationException(objectKey, "stat", e);
        }
    }
}
