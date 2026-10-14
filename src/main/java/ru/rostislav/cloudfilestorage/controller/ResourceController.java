package ru.rostislav.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;
import ru.rostislav.cloudfilestorage.mapper.ResourceMapper;
import ru.rostislav.cloudfilestorage.service.FileStorageService;

import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/resource")
@RestController
public class ResourceController {

    private final FileStorageService fileStorageService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResourceResponse getResourceInfo(
            @RequestParam String path
    ) {
        ResourceInfo info = fileStorageService.getResourceInfo(path);
        return ResourceMapper.toResponse(info);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(
            @RequestParam String path
    ) {
        fileStorageService.deleteResource(path);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadResource(
            @RequestParam String path
    ) {
        InputStream inputStream = fileStorageService.downloadResource(path);
        Resource resource = new InputStreamResource(inputStream);
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/move")
    @ResponseStatus(HttpStatus.OK)
    public ResourceResponse moveResource(
            @RequestParam String from,
            @RequestParam String to
    ) {
        ResourceInfo info = fileStorageService.moveResource(from, to);
        return ResourceMapper.toResponse(info);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceResponse> searchResource(
            @RequestParam String query
    ) {
        List<ResourceInfo> infos = fileStorageService.searchResource(query);
        return ResourceMapper.toResponseList(infos);
    }

    @PostMapping
    public ResponseEntity<List<ResourceResponse>> uploadResource(
            @RequestParam("object") List<MultipartFile> files,
            @RequestParam String path
    ) {
        List<ResourceInfo> infos = fileStorageService.uploadFiles(path, files);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResourceMapper.toResponseList(infos));
    }

}
