package ru.rostislav.cloudfilestorage.dto.resource;

public record ResourceDTO(
        String path,
        String name,
        Long size,
        ResourceType type
) {
}
