package ru.rostislav.cloudfilestorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ru.rostislav.cloudfilestorage.dto.ArchiveEntry;
import ru.rostislav.cloudfilestorage.dto.resource.DownloadResult;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;
import ru.rostislav.cloudfilestorage.dto.storage.StorageObject;
import ru.rostislav.cloudfilestorage.exception.EmptyFileException;
import ru.rostislav.cloudfilestorage.exception.InvalidPathException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectAlreadyExistsException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectNotFoundException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectOperationException;
import ru.rostislav.cloudfilestorage.mapper.ResourceMapper;
import ru.rostislav.cloudfilestorage.security.UserDetailsImpl;
import ru.rostislav.cloudfilestorage.util.PathUtil;
import ru.rostislav.cloudfilestorage.util.PathValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FileStorageService {

    private final MinioService minioService;
    private final ArchiveService archiveService;

    public ResourceResponse uploadFile(String path, String filename,
                                   String contentType, InputStream in) {
        validateFolderPath(path);
        String normalizedPath = PathUtil.normalizeFolderPath(path);
        validateFilename(filename);

        String fullPath = normalizedPath + filename;
        checkObjectNotExists(fullPath);

        PushbackInputStream pushback = new PushbackInputStream(in, 1);
        try {
            int first = pushback.read();
            if (first == -1) {
                throw new EmptyFileException(filename);
            }
            pushback.unread(first);
        } catch (IOException e) {
            throw new ObjectOperationException(fullPath, "upload", e);
        }

        minioService.putObject(pushback, contentType, PathUtil.getStoragePath(fullPath, getUserId()));
        return getResourceInfo(fullPath);
    }

    public ResourceResponse createEmptyFolder(String folderName) {
        validateFolderPath(folderName);
        String normalizedFolderKey = PathUtil.normalizeFolderPath(folderName);
        checkParentFolderExist(normalizedFolderKey);
        checkFolderNotExists(normalizedFolderKey);
        minioService.putEmptyObject(PathUtil.getStoragePath(normalizedFolderKey, getUserId()));
        return getResourceInfo(normalizedFolderKey);
    }

    public void renameFile(String oldObjectKey, String newObjectKey) {
        validatePath(oldObjectKey);
        validatePath(newObjectKey);
        if (PathValidator.isFolderPath(newObjectKey)) {
            throw new InvalidPathException(newObjectKey);
        }
        checkObjectExists(oldObjectKey);
        checkObjectNotExists(newObjectKey);
        minioService.copyObject(PathUtil.getStoragePath(oldObjectKey, getUserId()), PathUtil.getStoragePath(newObjectKey, getUserId()));
        minioService.removeObject(PathUtil.getStoragePath(oldObjectKey, getUserId()));
    }

    public void renameFolder(String sourceFolder, String destFolder) {
        validateFolderPath(sourceFolder);
        validateFolderPath(destFolder);

        String normalizedSourceFolder = PathUtil.normalizeFolderPath(sourceFolder);
        String normalizedDestFolder = PathUtil.normalizeFolderPath(destFolder);

        checkFolderExists(normalizedSourceFolder);
        checkFolderNotExists(normalizedDestFolder);

        List<StorageObject> oldObjectList = minioService.getObjects(PathUtil.getStoragePath(normalizedSourceFolder, getUserId()), true);

        for (StorageObject object : oldObjectList) {
            String oldObjectKey = PathUtil.removeUserRootPath(object.objectKey(), getUserId());
            String relative = oldObjectKey.substring(normalizedSourceFolder.length());
            String newObjectKey = normalizedDestFolder + relative;

            minioService.copyObject(PathUtil.getStoragePath(oldObjectKey, getUserId()), PathUtil.getStoragePath(newObjectKey, getUserId()));
        }

        for (StorageObject object : oldObjectList) {
            String oldObjectKey = PathUtil.removeUserRootPath(object.objectKey(), getUserId());

            minioService.removeObject(PathUtil.getStoragePath(oldObjectKey, getUserId()));
        }
    }

    public ResourceResponse moveResource(String source, String dest) {
        if (source.endsWith("/")) {
            renameFolder(source, dest);
        } else {
            renameFile(source, dest);
        }
        return getResourceInfo(dest);
    }

    public StreamingResponseBody downloadFile(String objectKey) {
        validatePath(objectKey);
        checkObjectExists(objectKey);
        String storagePath = PathUtil.getStoragePath(objectKey, getUserId());
        return out -> {
            try (InputStream in = minioService.getObject(storagePath)) {
                in.transferTo(out);
            }
        };
    }

    public StreamingResponseBody downloadFolder(String folderKey) {
        validateFolderPath(folderKey);
        String normalizedFolderKey = PathUtil.normalizeFolderPath(folderKey);
        checkFolderExists(normalizedFolderKey);

        Long userId = getUserId();
        List<ArchiveEntry> entries = new ArrayList<>();

        for (StorageObject object : minioService.getObjects(PathUtil.getStoragePath(normalizedFolderKey, userId), true)) {
            String objectKey = PathUtil.removeUserRootPath(object.objectKey(), userId);
            if (objectKey.equals(normalizedFolderKey)) {
                continue;
            }

            String relativePath = objectKey.substring(normalizedFolderKey.length());
            String storagePath = PathUtil.getStoragePath(objectKey, userId);

            entries.add(new ArchiveEntry(
                    relativePath,
                    object.type(),
                    object.type() == ResourceType.FILE
                            ? () -> minioService.getObject(storagePath)
                            : null
            ));

        }
        return out -> archiveService.writeToZip(entries, out);
    }

    public DownloadResult downloadResource(String path) {
        validatePath(path);
        if (path.isEmpty()) {
            throw new InvalidPathException(path);
        }

        boolean isFolder = PathValidator.isFolderPath(path);

        StreamingResponseBody body = isFolder
                ? downloadFolder(path)
                : downloadFile(path);

        return new DownloadResult(buildDownloadName(path, isFolder), body);
    }

    private String buildDownloadName(String path, boolean isFolder) {
        String trimmed = isFolder
                ? path.substring(0, path.length() - 1)
                : path;

        String name = trimmed.substring(trimmed.lastIndexOf('/') + 1);
        return isFolder ? name + ".zip" : name;
    }

    public void deleteResource(String path) {
        if (PathValidator.isFolderPath(path)) {
            deleteFolder(path);
        } else {
            deleteFile(path);
        }
    }

    public void deleteFile(String objectKey) {
        validatePath(objectKey);
        checkObjectExists(objectKey);
        minioService.removeObject(PathUtil.getStoragePath(objectKey, getUserId()));
    }

    public void deleteFolder(String path) {
        validateFolderPath(path);
        checkFolderExists(path);
        List<StorageObject> objects = minioService.getObjects(PathUtil.getStoragePath(path, getUserId()), true);
        for (StorageObject object : objects) {
            minioService.removeObject(object.objectKey());
        }
    }

    public List<ResourceResponse> getDirectoryContent(String path) {
        validateFolderPath(path);
        String normalizedPath = PathUtil.normalizeFolderPath(path);
        checkFolderExists(normalizedPath);

        List<ResourceInfo> infos = new ArrayList<>();

        for (StorageObject object : minioService.getObjects(PathUtil.getStoragePath(normalizedPath, getUserId()), false)) {
            String objectKey = PathUtil.removeUserRootPath(object.objectKey(), getUserId());

            if (objectKey.equals(normalizedPath)) {
                continue;
            }

            if (object.type() == ResourceType.DIRECTORY) {
                String directoryPath = objectKey.substring(0, objectKey.length() - 1);

                infos.add(new ResourceInfo(
                        PathUtil.extractPath(directoryPath),
                        PathUtil.extractName(directoryPath),
                        null,
                        ResourceType.DIRECTORY
                ));
            }

            if (object.type() == ResourceType.FILE) {
                infos.add(new ResourceInfo(
                        PathUtil.extractPath(objectKey),
                        PathUtil.extractName(objectKey),
                        object.size(),
                        ResourceType.FILE
                ));
            }
        }

        return ResourceMapper.toResponseList(infos);
    }

    public ResourceResponse getResourceInfo(String path) {
        if (PathValidator.isFolderPath(path)) {
            return ResourceMapper.toResponse(getFolderInfo(path));
        } else {
            return ResourceMapper.toResponse(getFileInfo(path));
        }
    }

    public List<ResourceResponse> searchResource(String query) {
        List<StorageObject> objects = minioService.getObjects(PathUtil.getStoragePath("", getUserId()), true);
        List<ResourceInfo> results = new ArrayList<>();
        for (StorageObject object : objects) {
            String path = PathUtil.removeUserRootPath(object.objectKey(), getUserId());
            if (path.toLowerCase().contains(query.toLowerCase())) {

                if (object.type() == ResourceType.DIRECTORY) {
                    String directoryPath = path.substring(0, path.length() - 1);

                    results.add(
                            new ResourceInfo(
                                    PathUtil.extractPath(directoryPath),
                                    PathUtil.extractName(directoryPath),
                                    null,
                                    object.type()
                            )
                    );
                }

                if (object.type() == ResourceType.FILE) {
                    results.add(
                            new ResourceInfo(
                                    PathUtil.extractPath(path),
                                    PathUtil.extractName(path),
                                    object.size(),
                                    object.type()
                            )
                    );
                }
            }
        }
        return ResourceMapper.toResponseList(results);
    }

    private ResourceInfo getFileInfo(String path) {
        validatePath(path);

        try {
            StorageObject object = minioService.getObjectStat(
                    PathUtil.getStoragePath(path, getUserId())
            );

            return new ResourceInfo(
                    PathUtil.extractPath(path),
                    PathUtil.extractName(path),
                    object.size(),
                    object.type()
            );
        } catch (ObjectNotFoundException exception) {
            throw new ObjectNotFoundException(path);
        }
    }

    private ResourceInfo getFolderInfo(String path) {
        validateFolderPath(path);
        String normalizedPath = PathUtil.normalizeFolderPath(path);

        checkFolderExists(normalizedPath);

        String directoryPath = normalizedPath.substring(0, normalizedPath.length() - 1);

        return new ResourceInfo(
                PathUtil.extractPath(directoryPath),
                PathUtil.extractName(directoryPath),
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
        if (minioService.isObjectExist(PathUtil.getStoragePath(objectKey, getUserId()))) {
            throw new ObjectAlreadyExistsException(objectKey);
        }
    }

    private void checkObjectExists(String oldObjectKey) {
        if (!minioService.isObjectExist(PathUtil.getStoragePath(oldObjectKey, getUserId()))) {
            throw new ObjectNotFoundException(oldObjectKey);
        }
    }

    private void checkFolderNotExists(String folderPath) {
        if (minioService.isFolderExist(PathUtil.getStoragePath(folderPath, getUserId()))) {
            throw new ObjectAlreadyExistsException(folderPath);
        }
    }

    private void checkFolderExists(String folderPath) {
        if (folderPath.isEmpty()) {
            return;
        }
        if (!minioService.isFolderExist(PathUtil.getStoragePath(folderPath, getUserId()))) {
            throw new ObjectNotFoundException(folderPath);
        }
    }

    private Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId();
    }

    private void validatePath(String path) {
        if (!PathValidator.isValidPath(path)) {
            throw new InvalidPathException(path);
        }
    }

    private void validateFolderPath(String path) {
        validatePath(path);

        if (!PathValidator.isFolderPath(path)) {
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
