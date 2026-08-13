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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Service
public class FileStorageService {

    private final MinioService minioService;

    public List<ResourceInfo> uploadFiles(String path, List<MultipartFile> files) {
        List<ResourceInfo> infoList = new ArrayList<>();
        String normalizedPath = normalizePath(path);
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new EmptyFileException(file.getOriginalFilename());
            }
            String fullPath = normalizedPath + file.getOriginalFilename();
            checkObjectNotExist(fullPath);
            minioService.putObject(file, fullPath);
            infoList.add(getResourceInfo(fullPath));
        }
        return infoList;
    }

    public void createEmptyFolder(String folderKey) {
        String normalizedFolderKey = normalizePath(folderKey);
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
        String normalizedOldFolderKey = normalizePath(oldFolderKey);
        String normalizedNewFolderKey = normalizePath(newFolderKey);
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

    public void moveResource(String source, String dest) {
        if (source.endsWith("/")) {
            renameFolder(source, dest);
        } else {
            renameFile(source, dest);
        }
    }

    public InputStream downloadFile(String objectKey) {
        checkObjectExist(objectKey);
        return minioService.getObject(objectKey);
    }

    public InputStream downloadFolder(String folderKey) {
        String normalizedFolderKey = normalizePath(folderKey);

        checkObjectExist(normalizedFolderKey);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

            for (Result<Item> result : minioService.getObjectList(normalizedFolderKey)) {
                Item item = result.get();
                String objectKey = item.objectName();
                if (objectKey.equals(normalizedFolderKey)) {
                    continue;
                }
                String relativePath = objectKey.substring(normalizedFolderKey.length());

                ZipEntry zipEntry = new ZipEntry(relativePath);
                zipOutputStream.putNextEntry(zipEntry);
                try (InputStream inputStream = minioService.getObject(objectKey)) {
                    inputStream.transferTo(zipOutputStream);
                }
                zipOutputStream.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ZIP archive", e);
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public InputStream downloadResource(String path) {
        if (path.endsWith("/")) {
            return downloadFolder(path);
        } else {
            return downloadFile(path);
        }
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

    private String normalizePath(String folder) {
        return folder.endsWith("/") ? folder : folder + "/";
    }
}
