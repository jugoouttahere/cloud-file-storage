package ru.rostislav.cloudfilestorage.dto;

import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;

import java.io.InputStream;
import java.util.function.Supplier;

public record ArchiveEntry(
        String path,
        ResourceType type,
        Supplier<InputStream> content
) {
}
