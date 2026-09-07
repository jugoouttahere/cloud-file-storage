package ru.rostislav.cloudfilestorage.dto;

import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;

import java.io.InputStream;

public record ArchiveEntry(
        String path,
        ResourceType type,
        InputStream content
) {
}
