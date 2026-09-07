package ru.rostislav.cloudfilestorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rostislav.cloudfilestorage.dto.ArchiveEntry;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;
import ru.rostislav.cloudfilestorage.dto.storage.StorageObject;
import ru.rostislav.cloudfilestorage.exception.EmptyFileException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectAlreadyExistsException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectNotFoundException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FileStorageService {

    private final MinioService minioService;
    private final ArchiveService archiveService;

    public List<ResourceInfo> uploadFiles(String path, List<MultipartFile> files) {
        List<ResourceInfo> infoList = new ArrayList<>();
        String normalizedPath = normalizeFolderPath(path);
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new EmptyFileException(file.getOriginalFilename());
            }
            String fullPath = normalizedPath + file.getOriginalFilename();
            checkObjectNotExists(fullPath);
            minioService.putObject(file, fullPath);
            infoList.add(getResourceInfo(fullPath));
        }
        return infoList;
    }

    public ResourceInfo createEmptyFolder(String folderName) {
        String normalizedFolderKey = normalizeFolderPath(folderName);
        checkParentFolderExist(normalizedFolderKey);
        checkFolderNotExists(normalizedFolderKey);
        minioService.putEmptyObject(normalizedFolderKey);

        return getResourceInfo(normalizedFolderKey);
    }

    public void renameFile(String oldObjectKey, String newObjectKey) {
        checkObjectExists(oldObjectKey);
        checkObjectNotExists(newObjectKey);
        minioService.copyObject(oldObjectKey, newObjectKey);
        minioService.removeObject(oldObjectKey);
    }

    public void renameFolder(String sourceFolder, String destFolder) {
        String normalizedSourceFolder = normalizeFolderPath(sourceFolder);
        String normalizedDestFolder = normalizeFolderPath(destFolder);
        checkFolderExists(normalizedSourceFolder);
        checkFolderNotExists(normalizedDestFolder);

        List<StorageObject> oldObjectList = minioService.getObjects(normalizedSourceFolder, true);
        for (StorageObject object : oldObjectList) {
            String oldObjectKey = object.objectKey();
            String relative = oldObjectKey.substring(normalizedSourceFolder.length());
            String newObjectKey = normalizedDestFolder + relative;

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
        checkObjectExists(objectKey);
        return minioService.getObject(objectKey);
    }

    public InputStream downloadFolder(String folderKey) {
        String normalizedFolderKey = normalizeFolderPath(folderKey);
        checkFolderExists(normalizedFolderKey);

        List<ArchiveEntry> entries = new ArrayList<>();

        for (StorageObject object : minioService.getObjects(normalizedFolderKey, true)) {
            String objectKey = object.objectKey();
            if (objectKey.equals(normalizedFolderKey)) {
                continue;
            }
            String relativePath = objectKey.substring(normalizedFolderKey.length());

            if (object.type() == ResourceType.FILE) {
                entries.add(new ArchiveEntry(
                        relativePath,
                        object.type(),
                        minioService.getObject(objectKey)
                        ));
            }

            if (object.type() == ResourceType.DIRECTORY) {
                entries.add(new ArchiveEntry(
                        relativePath,
                        object.type(),
                        null
                ));
            }

        }
        return archiveService.createZip(entries);
    }

    public InputStream downloadResource(String path) {
        if (path.endsWith("/")) {
            return downloadFolder(path);
        } else {
            return downloadFile(path);
        }
    }

    public void deleteFile(String objectKey) {
        checkObjectExists(objectKey);
        minioService.removeObject(objectKey);
    }

    public List<ResourceInfo> getDirectoryContent(String path) {
        String normalizedPath = normalizeFolderPath(path);
        checkFolderExists(normalizedPath);

        List<ResourceInfo> infos = new ArrayList<>();

        for (StorageObject object : minioService.getObjects(normalizedPath, false)) {
            String objectKey = object.objectKey();

            if (object.type() == ResourceType.DIRECTORY) {
                String directoryPath = objectKey.substring(0, objectKey.length() - 1);

                infos.add(new ResourceInfo(
                        extractPath(directoryPath),
                        extractName(directoryPath),
                        null,
                        ResourceType.DIRECTORY
                ));
            }

            if (object.type() == ResourceType.FILE) {
                infos.add(new ResourceInfo(
                        extractPath(objectKey),
                        extractName(objectKey),
                        object.size(),
                        ResourceType.FILE
                ));
            }
        }

        return infos;
    }

    public ResourceInfo getResourceInfo(String path) {
        if (path.endsWith("/")) {
            return getFolderInfo(path);
        } else {
            return getFileInfo(path);
        }
    }

    private ResourceInfo getFileInfo(String path) {
        StorageObject object = minioService.getObjectStat(path);

        return new ResourceInfo(
                extractPath(object.objectKey()),
                extractName(object.objectKey()),
                object.size(),
                object.type()
        );
    }

    private ResourceInfo getFolderInfo(String path) {
        String normalizedPath = normalizeFolderPath(path);

        checkFolderExists(normalizedPath);

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
        checkFolderExists(parentPath);
    }

    private String extractPath(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index == -1 ? "" : objectKey.substring(0, index + 1);
    }

    private String extractName(String objectKey) {
        int index = objectKey.lastIndexOf("/");
        return index == -1 ? objectKey : objectKey.substring(index + 1);
    }

    private void checkObjectNotExists(String objectKey) {
        if (minioService.isObjectExist(objectKey)) {
            throw new ObjectAlreadyExistsException(objectKey);
        }
    }

    private void checkObjectExists(String oldObjectKey) {
        if (!minioService.isObjectExist(oldObjectKey)) {
            throw new ObjectNotFoundException(oldObjectKey);
        }
    }

    private void checkFolderNotExists(String folderPath) {
        if (minioService.isFolderExist(folderPath)) {
            throw new ObjectAlreadyExistsException(folderPath);
        }
    }

    private void checkFolderExists(String folderPath) {
        if (!minioService.isFolderExist(folderPath)) {
            throw new ObjectNotFoundException(folderPath);
        }
    }

    private String normalizeFolderPath(String folder) {
        return folder.endsWith("/") ? folder : folder + "/";
    }
}
