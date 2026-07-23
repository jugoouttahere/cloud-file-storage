package ru.rostislav.cloudfilestorage.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rostislav.cloudfilestorage.util.MinioProperties;

@RequiredArgsConstructor
@Service
public class MinioService {

    private final MinioClient minioClient;

    private final MinioProperties properties;

    @SneakyThrows
    public void uploadFile(MultipartFile file, String objectKey) {
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
}
