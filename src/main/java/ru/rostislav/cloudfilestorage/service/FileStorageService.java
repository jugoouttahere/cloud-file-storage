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

    public ResourceInfo createEmptyFolder(String folderKey) {
        String normalizedFolderKey = normalizePath(folderKey);
        checkParentFolderExist(normalizedFolderKey);
        checkFolderNotExist(normalizedFolderKey);
        minioService.putEmptyObject(normalizedFolderKey);

        return getResourceInfo(normalizedFolderKey);
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
        checkFolderExist(normalizedOldFolderKey);
        checkFolderNotExist(normalizedNewFolderKey);

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

    public ResourceInfo moveResource(String source, String dest) {
        if (source.endsWith("/")) {
            renameFolder(source, dest);
        } else {
            renameFile(source, dest);
        }
        return getResourceInfo(dest);
    }

    public InputStream downloadFile(String objectKey) {
        checkObjectExist(objectKey);
        return minioService.getObject(objectKey);
    }

    public InputStream downloadFolder(String folderKey) {
        String normalizedFolderKey = normalizePath(folderKey);

        checkFolderExist(normalizedFolderKey);

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

    public List<ResourceInfo> getDirectoryContent(String path) {
        String normalizedPath = normalizePath(path);
        checkFolderExist(normalizedPath);

        List<ResourceInfo> infos = new ArrayList<>();

        for (Result<Item> result : minioService.getDirectoryContent(normalizedPath)) {
            try {
                Item item = result.get();
                String objectKey = item.objectName();

                if (item.isDir()) {
                    String directoryPath = objectKey.substring(0, objectKey.length() - 1);

                    infos.add(new ResourceInfo(
                            extractPath(directoryPath),
                            extractName(directoryPath),
                            null,
                            ResourceType.DIRECTORY
                    ));
                } else {
                    infos.add(new ResourceInfo(
                            extractPath(objectKey),
                            extractName(objectKey),
                            item.size(),
                            ResourceType.FILE
                    ));
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return infos;
    }

    public ResourceInfo getResourceInfo(String path) {
        if (path.endsWith("/")) {
            return getFolderInfo(path);
        }

        return getFileInfo(path);
    }

    private ResourceInfo getFileInfo(String path) {
        StatObjectResponse stat = minioService.getObjectStat(path);

        return new ResourceInfo(
                extractPath(stat.object()),
                extractName(stat.object()),
                stat.size(),
                ResourceType.FILE
        );
    }

    private ResourceInfo getFolderInfo(String path) {
        String normalizedPath = normalizePath(path);

        checkFolderExist(normalizedPath);

        String directoryPath = normalizedPath.substring(0, normalizedPath.length() - 1);

        return new ResourceInfo(
                extractPath(directoryPath),
                extractName(directoryPath),
                null,
                ResourceType.DIRECTORY
        );
    }

    private void checkParentFolderExist(String folderPath) {
        int index = folderPath.lastIndexOf("/", folderPath.length() - 2);
        if (index == -1) {
            return;
        }
        String parentPath = folderPath.substring(0, index + 1);
        checkFolderExist(parentPath);
    }

    private static ResourceType getResourceType(String path) {
        return path.endsWith("/") ? ResourceType.DIRECTORY : ResourceType.FILE;
    }

    private String extractPath(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index == -1 ? "" : objectKey.substring(0, index + 1);
    }

    private String extractName(String objectKey) {
        int index = objectKey.lastIndexOf("/");
        return index == -1 ? objectKey : objectKey.substring(index + 1);
    }

    private void checkFolderExist(String folderPath) {
        Iterable<Result<Item>> objects =
                minioService.getObjectList(normalizePath(folderPath));

        if (!objects.iterator().hasNext()) {
            throw new ObjectNotFoundException(folderPath);
        }
    }

    private void checkFolderNotExist(String folderPath) {
        Iterable<Result<Item>> objects =
                minioService.getObjectList(normalizePath(folderPath));

        if (objects.iterator().hasNext()) {
            throw new ObjectAlreadyExistsException(folderPath);
        }
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
