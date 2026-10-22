package ru.rostislav.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;
import ru.rostislav.cloudfilestorage.service.FileStorageService;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/directory")
@RestController
public class DirectoryController {

    private final FileStorageService fileStorageService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceResponse> getDirectory(
            @RequestParam String path
    ) {
        return fileStorageService.getDirectoryContent(path);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponse createDirectory(
            @RequestParam String path
    ) {
        return fileStorageService.createEmptyFolder(path);
    }
}
