package ru.rostislav.cloudfilestorage.dto.storage;

import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;

public record StorageObject(
        String objectKey,
        Long size,
        ResourceType type
) {
}
