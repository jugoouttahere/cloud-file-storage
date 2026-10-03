package ru.rostislav.cloudfilestorage.util;

import org.springframework.stereotype.Component;

@Component
public class PathUtil {

    public String getUserRootPath(Long userId) {
        return String.format("user-%d-files/", userId);
    }

    public String removeUserRootPath(String path, Long userId) {
        String userRootPath = getUserRootPath(userId);
        if (path.startsWith(userRootPath)) {
            return path.substring(userRootPath.length());
        } else {
            throw new IllegalArgumentException("Wrong root path");
        }
    }

    public String getStoragePath(String path, Long userId) {
        return getUserRootPath(userId) + path;
    }

    public String normalizeFolderPath(String folder) {
        if (folder.isEmpty()) {
            return folder;
        }

        return folder.endsWith("/") ? folder : folder + "/";
    }

    public String extractName(String objectKey) {
        int index = objectKey.lastIndexOf("/");
        return index == -1 ? objectKey : objectKey.substring(index + 1);
    }

    public String extractPath(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index == -1 ? "" : objectKey.substring(0, index + 1);
    }
}
