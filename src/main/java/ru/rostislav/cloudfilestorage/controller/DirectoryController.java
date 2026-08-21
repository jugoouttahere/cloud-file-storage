package ru.rostislav.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;
import ru.rostislav.cloudfilestorage.mapper.ResourceMapper;
import ru.rostislav.cloudfilestorage.service.FileStorageService;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/directory")
@RestController
public class DirectoryController {

    private final FileStorageService fileStorageService;

    private final ResourceMapper resourceMapper;

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getDirectory(
            @RequestParam String path
    ) {
        List<ResourceInfo> infos = fileStorageService.getDirectoryContent(path);
        return ResponseEntity
                .ok(resourceMapper.toResponseList(infos));
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> createDirectory(
            @RequestParam String path
    ) {
        ResourceInfo info = fileStorageService.createEmptyFolder(path);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resourceMapper.toResponse(info));
    }
}
