package ru.rostislav.cloudfilestorage.dto.resource;

public record ResourceInfo(
        String path,
        String name,
        Long size,
        ResourceType type
) {
}
