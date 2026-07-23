package ru.rostislav.cloudfilestorage.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rostislav.cloudfilestorage.util.MinioProperties;

import java.io.InputStream;

@RequiredArgsConstructor
@Service
public class MinioService {

    private final MinioClient minioClient;

    private final MinioProperties properties;

    @SneakyThrows
    public void putObject(MultipartFile file, String objectKey) {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectKey)
                        .stream(
                                file.getInputStream(),
                                file.getSize(),
                                -1
                        )
                        .contentType(file.getContentType())
                        .build()
        );
    }

    @SneakyThrows
    public InputStream getObject(String objectKey) {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectKey)
                        .build()
        );
    }

    @SneakyThrows
    public void removeObject(String objectKey) {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectKey)
                        .build()
        );
    }

    @SneakyThrows
    public void copyObject(String source, String dest) {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(dest)
                        .source(
                                CopySource.builder()
                                        .bucket(properties.getBucket())
                                        .object(source)
                                        .build())
                        .build()
        );
    }

    @SneakyThrows
    public StatObjectResponse getObjectStat(String objectKey) {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectKey)
                        .build()
        );
    }

    @SneakyThrows
    public boolean isObjectExist(String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
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

    public Iterable<Result<Item>> getObjectList(String path) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(properties.getBucket())
                        .prefix(path)
                        .recursive(true)
                        .build()
        );
    }
}
