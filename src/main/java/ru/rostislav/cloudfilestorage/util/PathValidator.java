package ru.rostislav.cloudfilestorage.util;

import org.springframework.stereotype.Component;

@Component
public class PathValidator {

    public boolean isValidPath(String path) {
        if (path == null) {
            return false;
        }

        if (path.isEmpty()) {
            return true;
        }

        if (
                path.isBlank() ||
                path.startsWith("/") ||
                path.contains("\\") ||
                path.contains("//")
        ) {
            return false;
        }

        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.equals(".") || part.equals("..")) {
                return false;
            }
        }
        return true;
    }

    public boolean isFolderPath(String path) {
        return path != null && (path.isEmpty() || path.endsWith("/"));
    }
}
