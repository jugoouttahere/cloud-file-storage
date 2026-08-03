package ru.rostislav.cloudfilestorage.service;

import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;
import ru.rostislav.cloudfilestorage.exception.minio.EmptyFileException;
import ru.rostislav.cloudfilestorage.exception.minio.GetObjectException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectAlreadyExistsException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectNotFoundException;

import java.io.InputStream;

@RequiredArgsConstructor
@Service
public class FileStorageService {

    private final MinioService minioService;

    public void uploadFile(String objectKey, MultipartFile file) {
        if (file.isEmpty()) {
            throw new EmptyFileException(objectKey);
        }
        checkObjectNotExist(objectKey);
        minioService.putObject(file, objectKey);
    }

    public void createEmptyFolder(String folderKey) {
        String normalizedFolderKey = normalizeFolder(folderKey);
        checkObjectNotExist(normalizedFolderKey);
        minioService.putEmptyObject(normalizedFolderKey);
    }

    public void renameFile(String oldObjectKey, String newObjectKey) {
        checkObjectExist(oldObjectKey);
        checkObjectNotExist(newObjectKey);
        minioService.copyObject(oldObjectKey, newObjectKey);
        minioService.removeObject(oldObjectKey);
    }

    public void renameFolder(String oldFolderKey, String newFolderKey) {
        String normalizedOldFolderKey = normalizeFolder(oldFolderKey);
        String normalizedNewFolderKey = normalizeFolder(newFolderKey);
        Iterable<Result<Item>> oldObjectList = minioService.getObjectList(normalizedOldFolderKey);
        for (Result<Item> object : oldObjectList) {
            String oldObjectKey = null;
            try {
                oldObjectKey = object.get().objectName();
            } catch (Exception e) {
                throw new GetObjectException("listing objects in folder: " + oldFolderKey, e);
            }
            String relative = oldObjectKey.substring(normalizedOldFolderKey.length());
            String newObjectKey = normalizedNewFolderKey + relative;
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

    public ResourceInfo getResourceInfo(String path) {
        StatObjectResponse stat = minioService.getObjectStat(path);
        return new ResourceInfo(
                extractPath(stat.object()),
                extractName(stat.object()),
                stat.size(),
                ResourceType.FILE
        );
    }

    private String extractPath(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index == -1 ? "" : objectKey.substring(0, index + 1);
    }

    private String extractName(String objectKey) {
        int index = objectKey.lastIndexOf("/");
        return index == -1 ? objectKey : objectKey.substring(index + 1);
    }

    private void checkObjectNotExist(String objectKey) {
        if (minioService.isObjectExist(objectKey)) {
            throw new ObjectAlreadyExistsException(objectKey);
        }
    }

    private void checkObjectExist(String oldObjectKey) {
        if (!minioService.isObjectExist(oldObjectKey)) {
            throw new ObjectNotFoundException(oldObjectKey);
        }
    }

    private String normalizeFolder(String folder) {
        return folder.endsWith("/") ? folder : folder + "/";
    }
}
