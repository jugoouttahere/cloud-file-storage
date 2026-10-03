package ru.rostislav.cloudfilestorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rostislav.cloudfilestorage.dto.ArchiveEntry;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;
import ru.rostislav.cloudfilestorage.dto.storage.StorageObject;
import ru.rostislav.cloudfilestorage.exception.EmptyFileException;
import ru.rostislav.cloudfilestorage.exception.InvalidPathException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectAlreadyExistsException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectNotFoundException;
import ru.rostislav.cloudfilestorage.security.UserDetailsImpl;
import ru.rostislav.cloudfilestorage.util.PathUtil;
import ru.rostislav.cloudfilestorage.util.PathValidator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FileStorageService {

    private final MinioService minioService;
    private final ArchiveService archiveService;
    private final PathUtil pathUtil;
    private final PathValidator pathValidator;

    public List<ResourceInfo> uploadFiles(String path, List<MultipartFile> files) {
        validateFolderPath(path);
        String normalizedPath = pathUtil.normalizeFolderPath(path);
        List<ResourceInfo> infoList = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            validateFilename(filename);
            if (file.isEmpty()) {
                throw new EmptyFileException(filename);
            }
            String fullPath = normalizedPath + filename;
            checkObjectNotExists(fullPath);
            minioService.putObject(file, pathUtil.getStoragePath(fullPath, getUserId()));
            infoList.add(getResourceInfo(fullPath));
        }
        return infoList;
    }

    public ResourceInfo createEmptyFolder(String folderName) {
        validateFolderPath(folderName);
        String normalizedFolderKey = pathUtil.normalizeFolderPath(folderName);
        checkParentFolderExist(normalizedFolderKey);
        checkFolderNotExists(normalizedFolderKey);
        minioService.putEmptyObject(pathUtil.getStoragePath(normalizedFolderKey, getUserId()));
        return getResourceInfo(normalizedFolderKey);
    }

    public void renameFile(String oldObjectKey, String newObjectKey) {
        validatePath(oldObjectKey);
        validatePath(newObjectKey);
        if (pathValidator.isFolderPath(newObjectKey)) {
            throw new InvalidPathException(newObjectKey);
        }
        checkObjectExists(oldObjectKey);
        checkObjectNotExists(newObjectKey);
        minioService.copyObject(pathUtil.getStoragePath(oldObjectKey, getUserId()), pathUtil.getStoragePath(newObjectKey, getUserId()));
        minioService.removeObject(pathUtil.getStoragePath(oldObjectKey, getUserId()));
    }

    public void renameFolder(String sourceFolder, String destFolder) {
        validateFolderPath(sourceFolder);
        validateFolderPath(destFolder);

        String normalizedSourceFolder = pathUtil.normalizeFolderPath(sourceFolder);
        String normalizedDestFolder = pathUtil.normalizeFolderPath(destFolder);

        checkFolderExists(normalizedSourceFolder);
        checkFolderNotExists(normalizedDestFolder);

        List<StorageObject> oldObjectList = minioService.getObjects(pathUtil.getStoragePath(normalizedSourceFolder, getUserId()), true);

        for (StorageObject object : oldObjectList) {
            String oldObjectKey = pathUtil.removeUserRootPath(object.objectKey(), getUserId());
            String relative = oldObjectKey.substring(normalizedSourceFolder.length());
            String newObjectKey = normalizedDestFolder + relative;

            minioService.copyObject(pathUtil.getStoragePath(oldObjectKey, getUserId()), pathUtil.getStoragePath(newObjectKey, getUserId()));
        }

        for (StorageObject object : oldObjectList) {
            String oldObjectKey = pathUtil.removeUserRootPath(object.objectKey(), getUserId());

            minioService.removeObject(pathUtil.getStoragePath(oldObjectKey, getUserId()));
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
        validatePath(objectKey);
        checkObjectExists(objectKey);
        return minioService.getObject(pathUtil.getStoragePath(objectKey, getUserId()));
    }

    public InputStream downloadFolder(String folderKey) {
        validateFolderPath(folderKey);
        String normalizedFolderKey = pathUtil.normalizeFolderPath(folderKey);
        checkFolderExists(normalizedFolderKey);

        List<ArchiveEntry> entries = new ArrayList<>();

        for (StorageObject object : minioService.getObjects(pathUtil.getStoragePath(normalizedFolderKey, getUserId()), true)) {
            String objectKey = pathUtil.removeUserRootPath(object.objectKey(), getUserId());
            if (objectKey.equals(normalizedFolderKey)) {
                continue;
            }
            String relativePath = objectKey.substring(normalizedFolderKey.length());

            if (object.type() == ResourceType.FILE) {
                entries.add(new ArchiveEntry(
                        relativePath,
                        object.type(),
                        minioService.getObject(pathUtil.getStoragePath(objectKey, getUserId()))
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
        if (pathValidator.isFolderPath(path)) {
            return downloadFolder(path);
        } else {
            return downloadFile(path);
        }
    }

    public void deleteResource(String path) {
        if (pathValidator.isFolderPath(path)) {
            deleteFolder(path);
        } else {
            deleteFile(path);
        }
    }

    public void deleteFile(String objectKey) {
        validatePath(objectKey);
        checkObjectExists(objectKey);
        minioService.removeObject(pathUtil.getStoragePath(objectKey, getUserId()));
    }

    public void deleteFolder(String path) {
        validateFolderPath(path);
        checkFolderExists(path);
        List<StorageObject> objects = minioService.getObjects(pathUtil.getStoragePath(path, getUserId()), true);
        for (StorageObject object : objects) {
            minioService.removeObject(object.objectKey());
        }
    }

    public List<ResourceInfo> getDirectoryContent(String path) {
        validateFolderPath(path);
        String normalizedPath = pathUtil.normalizeFolderPath(path);
        checkFolderExists(normalizedPath);

        List<ResourceInfo> infos = new ArrayList<>();

        for (StorageObject object : minioService.getObjects(pathUtil.getStoragePath(normalizedPath, getUserId()), false)) {
            String objectKey = pathUtil.removeUserRootPath(object.objectKey(), getUserId());

            if (objectKey.equals(normalizedPath)) {
                continue;
            }

            if (object.type() == ResourceType.DIRECTORY) {
                String directoryPath = objectKey.substring(0, objectKey.length() - 1);

                infos.add(new ResourceInfo(
                        pathUtil.extractPath(directoryPath),
                        pathUtil.extractName(directoryPath),
                        null,
                        ResourceType.DIRECTORY
                ));
            }

            if (object.type() == ResourceType.FILE) {
                infos.add(new ResourceInfo(
                        pathUtil.extractPath(objectKey),
                        pathUtil.extractName(objectKey),
                        object.size(),
                        ResourceType.FILE
                ));
            }
        }

        return infos;
    }

    public ResourceInfo getResourceInfo(String path) {
        if (pathValidator.isFolderPath(path)) {
            return getFolderInfo(path);
        } else {
            return getFileInfo(path);
        }
    }

    public List<ResourceInfo> searchResource(String query) {
        List<StorageObject> objects = minioService.getObjects(pathUtil.getStoragePath("", getUserId()), true);
        List<ResourceInfo> results = new ArrayList<>();
        for (StorageObject object : objects) {
            String path = pathUtil.removeUserRootPath(object.objectKey(), getUserId());
            if (path.toLowerCase().contains(query.toLowerCase())) {

                if (object.type() == ResourceType.DIRECTORY) {
                    String directoryPath = path.substring(0, path.length() - 1);

                    results.add(
                            new ResourceInfo(
                                    pathUtil.extractPath(directoryPath),
                                    pathUtil.extractName(directoryPath),
                                    null,
                                    object.type()
                            )
                    );
                }

                if (object.type() == ResourceType.FILE) {
                    results.add(
                            new ResourceInfo(
                                    pathUtil.extractPath(path),
                                    pathUtil.extractName(path),
                                    object.size(),
                                    object.type()
                            )
                    );
                }
            }
        }
        return results;
    }

    private ResourceInfo getFileInfo(String path) {
        validatePath(path);

        try {
            StorageObject object = minioService.getObjectStat(
                    pathUtil.getStoragePath(path, getUserId())
            );

            return new ResourceInfo(
                    pathUtil.extractPath(path),
                    pathUtil.extractName(path),
                    object.size(),
                    object.type()
            );
        } catch (ObjectNotFoundException exception) {
            throw new ObjectNotFoundException(path);
        }
    }

    private ResourceInfo getFolderInfo(String path) {
        validateFolderPath(path);
        String normalizedPath = pathUtil.normalizeFolderPath(path);

        checkFolderExists(normalizedPath);

        String directoryPath = normalizedPath.substring(0, normalizedPath.length() - 1);

        return new ResourceInfo(
                pathUtil.extractPath(directoryPath),
                pathUtil.extractName(directoryPath),
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

    private void checkObjectNotExists(String objectKey) {
        if (minioService.isObjectExist(pathUtil.getStoragePath(objectKey, getUserId()))) {
            throw new ObjectAlreadyExistsException(objectKey);
        }
    }

    private void checkObjectExists(String oldObjectKey) {
        if (!minioService.isObjectExist(pathUtil.getStoragePath(oldObjectKey, getUserId()))) {
            throw new ObjectNotFoundException(oldObjectKey);
        }
    }

    private void checkFolderNotExists(String folderPath) {
        if (minioService.isFolderExist(pathUtil.getStoragePath(folderPath, getUserId()))) {
            throw new ObjectAlreadyExistsException(folderPath);
        }
    }

    private void checkFolderExists(String folderPath) {
        if (folderPath.isEmpty()) {
            return;
        }
        if (!minioService.isFolderExist(pathUtil.getStoragePath(folderPath, getUserId()))) {
            throw new ObjectNotFoundException(folderPath);
        }
    }

    private Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId();
    }

    private void validatePath(String path) {
        if (!pathValidator.isValidPath(path)) {
            throw new InvalidPathException(path);
        }
    }

    private void validateFolderPath(String path) {
        validatePath(path);

        if (!pathValidator.isFolderPath(path)) {
            throw new InvalidPathException(path);
        }
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank() || filename.endsWith("/")) {
            throw new InvalidPathException(filename);
        }

        validatePath(filename);
    }
}
