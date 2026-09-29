package ru.rostislav.cloudfilestorage.mapper;

import org.springframework.stereotype.Component;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;

import java.util.List;

@Component
public class ResourceMapper {

    public ResourceResponse toResponse(ResourceInfo info) {
        return new ResourceResponse(
                info.path(),
                info.name(),
                info.size(),
                info.type()
        );
    }

    public List<ResourceResponse> toResponseList(List<ResourceInfo> infos) {
        return infos.stream()
                .map(this::toResponse)
                .toList();
    }
}
