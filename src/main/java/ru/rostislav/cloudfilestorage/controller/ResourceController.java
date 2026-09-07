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
@RequestMapping("/resource")
@RestController
public class ResourceController {

    private final FileStorageService fileStorageService;

    private final ResourceMapper resourceMapper;

    @GetMapping
    public ResponseEntity<ResourceResponse> getResourceInfo(
            @RequestParam String path
    ) {
        ResourceInfo info = fileStorageService.getResourceInfo(path);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resourceMapper.toResponse(info));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResource(
            @RequestParam String path
    ) {
        fileStorageService.deleteFile(path);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
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
    public ResponseEntity<ResourceResponse> moveResource(
            @RequestParam String from,
            @RequestParam String to
    ) {
        ResourceInfo info = fileStorageService.moveResource(from, to);
        ResourceResponse response = resourceMapper.toResponse(info);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceResponse>> searchResource(
            @RequestParam String query
    ) {
        ResourceInfo info = fileStorageService.getResourceInfo(query);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(List.of(resourceMapper.toResponse(info)));
    }

    @PostMapping
    public ResponseEntity<List<ResourceResponse>> uploadResource(
            @RequestParam List<MultipartFile> files,
            @RequestParam String path
    ) {
        List<ResourceInfo> infos = fileStorageService.uploadFiles(path, files);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resourceMapper.toResponseList(infos));
    }

}
