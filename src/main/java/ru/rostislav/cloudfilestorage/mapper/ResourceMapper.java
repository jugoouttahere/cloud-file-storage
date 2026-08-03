package ru.rostislav.cloudfilestorage.mapper;

import org.springframework.stereotype.Component;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;

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
}
