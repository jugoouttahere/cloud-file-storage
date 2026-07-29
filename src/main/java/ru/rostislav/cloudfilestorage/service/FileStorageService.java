package ru.rostislav.cloudfilestorage.service;

import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RequiredArgsConstructor
@Service
public class FileStorageService {

    private final MinioService minioService;

    public void uploadFile(MultipartFile file, String objectKey) {
        if (file.isEmpty()) {
            throw new RuntimeException(String.format("File:%s is empty.", objectKey));
        }
        checkObjectNotExist(objectKey);
        minioService.putObject(file, objectKey);
    }

    public void createEmptyFolder(String folderName) {
        checkObjectNotExist(folderName);
        minioService.putEmptyObject(folderName);
    }

    public void renameFile(String oldObjectKey, String newObjectKey) {
        checkObjectExist(oldObjectKey);
        checkObjectNotExist(newObjectKey);
        minioService.copyObject(oldObjectKey, newObjectKey);
        minioService.removeObject(oldObjectKey);
    }

    @SneakyThrows
    public void renameFolder(String oldFolderName, String newFolderName) {
        Iterable<Result<Item>> oldFileList = minioService.getObjectList(oldFolderName);
        for (Result<Item> file : oldFileList) {
            String oldObjectKey = file.get().objectName();
            String relative = oldObjectKey.substring(oldFolderName.length());
            String newObjectKey = newFolderName + relative;
            minioService.copyObject(oldObjectKey, newObjectKey);
            minioService.removeObject(oldObjectKey);
        }
    }

    public void moveFile(String source, String dest) {
        renameFile(source, dest);
    }

    public void moveFolder(String source, String dest) {
        renameFolder(source, dest);
    }

    public InputStream downloadFile(String objectKey) {
        checkObjectExist(objectKey);
        return minioService.getObject(objectKey);
    }

    public void deleteFile(String objectKey) {
        checkObjectExist(objectKey);
        minioService.removeObject(objectKey);
    }

    private void checkObjectNotExist(String objectKey) {
        if (minioService.isObjectExist(objectKey)) {
            throw new RuntimeException(String.format("File with name:%s already exist.", objectKey));
        }
    }

    private void checkObjectExist(String oldObjectKey) {
        if (!minioService.isObjectExist(oldObjectKey)) {
            throw new RuntimeException(String.format("File with name:%s not exist.", oldObjectKey));
        }
    }
}
