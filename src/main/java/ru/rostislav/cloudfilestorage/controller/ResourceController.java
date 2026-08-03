package ru.rostislav.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;
import ru.rostislav.cloudfilestorage.mapper.ResourceMapper;
import ru.rostislav.cloudfilestorage.service.FileStorageService;

@RequiredArgsConstructor
@RequestMapping("/resource")
@RestController
public class ResourceController {

    private final FileStorageService fileStorageService;

    private final ResourceMapper resourceMapper;

    @GetMapping
    public ResponseEntity<ResourceResponse> getResource(
            @RequestParam String path
    ) {
        ResourceInfo info = fileStorageService.getResourceInfo(path);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceMapper.toResponse(info));
    }
}
