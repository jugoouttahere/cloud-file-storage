package ru.rostislav.cloudfilestorage.mapper;

import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;

import java.util.List;

public class ResourceMapper {

    public static ResourceResponse toResponse(ResourceInfo info) {
        return new ResourceResponse(
                info.path(),
                info.name(),
                info.size(),
                info.type()
        );
    }

    public static List<ResourceResponse> toResponseList(List<ResourceInfo> infos) {
        return infos.stream()
                .map(ResourceMapper::toResponse)
                .toList();
    }
}
