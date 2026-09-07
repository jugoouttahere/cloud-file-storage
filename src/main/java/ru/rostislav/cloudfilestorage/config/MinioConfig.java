package ru.rostislav.cloudfilestorage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.rostislav.cloudfilestorage.util.MinioProperties;

@RequiredArgsConstructor
@EnableConfigurationProperties(MinioProperties.class)
@Configuration
public class MinioConfig {

    private final MinioProperties properties;

    @SneakyThrows
    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(properties.getUrl())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();

        boolean isBucketExist = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(properties.getBucket())
                        .build()
        );

        if (!isBucketExist) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.getBucket())
                    .build()
            );
        }

        return minioClient;
    }
}
