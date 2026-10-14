package ru.rostislav.cloudfilestorage.util;

public class PathUtil {

    public static String getUserRootPath(Long userId) {
        return String.format("user-%d-files/", userId);
    }

    public static String removeUserRootPath(String path, Long userId) {
        String userRootPath = getUserRootPath(userId);
        if (path.startsWith(userRootPath)) {
            return path.substring(userRootPath.length());
        } else {
            throw new IllegalArgumentException("Wrong root path");
        }
    }

    public static String getStoragePath(String path, Long userId) {
        return getUserRootPath(userId) + path;
    }

    public static String normalizeFolderPath(String folder) {
        if (folder.isEmpty()) {
            return folder;
        }

        return folder.endsWith("/") ? folder : folder + "/";
    }

    public static String extractName(String objectKey) {
        int index = objectKey.lastIndexOf("/");
        return index == -1 ? objectKey : objectKey.substring(index + 1);
    }

    public static String extractPath(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index == -1 ? "" : objectKey.substring(0, index + 1);
    }
}
