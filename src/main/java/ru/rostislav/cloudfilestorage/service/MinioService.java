package ru.rostislav.cloudfilestorage.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rostislav.cloudfilestorage.config.MinioProperties;
import ru.rostislav.cloudfilestorage.dto.storage.StorageObject;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectNotFoundException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectOperationException;
import ru.rostislav.cloudfilestorage.mapper.StorageMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final StorageMapper storageMapper;

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
            throw new ObjectOperationException(objectKey, "put", e);
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
            throw new ObjectOperationException(objectKey, "put", e);
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
            throw new ObjectOperationException(objectKey, "get", e);
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
            throw new ObjectOperationException(objectKey, "remove", e);
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
            throw new ObjectOperationException(dest, "copy", e);
        }
    }

    public StorageObject getObjectStat(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
            return storageMapper.toStorageObject(stat);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new ObjectNotFoundException(objectKey);
            }
            throw new ObjectOperationException(objectKey, "stat", e);
        } catch (Exception e) {
            throw new ObjectOperationException(objectKey, "stat", e);
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
            throw new ObjectOperationException(objectKey, "stat", e);
        }
    }

    public List<StorageObject> getObjects(String path, boolean isRecursive) {
        List<StorageObject> objectList = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(properties.getBucket())
                            .prefix(path)
                            .recursive(isRecursive)
                            .build()
            );

            for (Result<Item> object : results) {
                Item item = object.get();
                objectList.add(storageMapper.toStorageObject(item));
            }
        } catch (Exception e) {
            throw new ObjectOperationException(path, "get objects", e);
        }

        return objectList;
    }

    public boolean isFolderExist(String folderPath) {
        List<StorageObject> objects = getObjects(folderPath, true);
        return !objects.isEmpty();
    }
}
