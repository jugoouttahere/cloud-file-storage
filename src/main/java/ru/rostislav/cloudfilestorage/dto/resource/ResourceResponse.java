package ru.rostislav.cloudfilestorage.dto.resource;

public record ResourceResponse(
        String path,
        String name,
        Long size,
        ResourceType type
) {
}
