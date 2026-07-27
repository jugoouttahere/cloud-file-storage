package ru.rostislav.cloudfilestorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FileStorageService {

    private final MinioService minioService;

    public void uploadFile() {

    }

    public void createEmptyFolder() {

    }

    public void renameFile(String oldObjectKey, String newObjectKey) {
        if (!minioService.isObjectExist(oldObjectKey)) {
            throw new RuntimeException(String.format("File with name:%s not exist.", oldObjectKey));
        }
        if (minioService.isObjectExist(newObjectKey)) {
            throw new RuntimeException(String.format("File with name:%s already exist.", newObjectKey));
        }
        minioService.copyObject(oldObjectKey, newObjectKey);
        minioService.removeObject(oldObjectKey);
    }

    public void renameFolder() {

    }

    public void moveFile() {

    }

    public void moveFolder() {

    }

    public void downloadFile() {

    }

    public void deleteFile() {

    }
}
