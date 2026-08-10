package ru.rostislav.cloudfilestorage.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rostislav.cloudfilestorage.exception.minio.*;
import ru.rostislav.cloudfilestorage.util.MinioProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@RequiredArgsConstructor
@Service
public class MinioService {

    private final MinioClient minioClient;

    private final MinioProperties properties;

    public void putObject(MultipartFile file, String objectKey) {
        try {
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
        } catch (Exception e) {
            throw new PutObjectException(objectKey, e);
        }
    }

    public void putEmptyObject(String objectKey) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .stream(
                                    new ByteArrayInputStream(new byte[0]),
                                    0,
                                    -1
                            )
                            .build()
            );
        } catch (Exception e) {
            throw new PutObjectException(objectKey, e);
        }
    }

    public InputStream getObject(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new GetObjectException(objectKey, e);
        }
    }

    public void removeObject(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RemoveObjectException(objectKey, e);
        }
    }

    public void copyObject(String source, String dest) {
        try {
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
        } catch (Exception e) {
            throw new CopyObjectException(dest, e);
        }
    }

    public StatObjectResponse getObjectStat(String objectKey) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new ObjectNotFoundException(objectKey);
            }
            throw new StatObjectException(objectKey, e);
        } catch (Exception e) {
            throw new StatObjectException(objectKey, e);
        }
    }

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
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new RuntimeException("Failed to check object existence", e);
        } catch (Exception e) {
            throw new StatObjectException(objectKey, e);
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
