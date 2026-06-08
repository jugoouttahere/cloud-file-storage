package ru.rostislav.cloudfilestorage.dto;

public record ResourceDTO(
        String path,
        String name,
        Long size,
        ResourceType type
) {
}
