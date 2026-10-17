package ru.rostislav.cloudfilestorage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ru.rostislav.cloudfilestorage.controller.upload.UploadHandler;
import ru.rostislav.cloudfilestorage.dto.resource.DownloadResult;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;
import ru.rostislav.cloudfilestorage.mapper.ResourceMapper;
import ru.rostislav.cloudfilestorage.service.FileStorageService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/resource")
@RestController
public class ResourceController {

    private final FileStorageService fileStorageService;
    private final UploadHandler uploadHandler;

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
    public ResponseEntity<StreamingResponseBody> downloadResource(
            @RequestParam String path
    ) {
        DownloadResult result = fileStorageService.downloadResource(path);
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(result.filename(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(result.body());
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
            HttpServletRequest request,
            @RequestParam String path
    ) {
        List<ResourceInfo> infos = uploadHandler.handle(request, path);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResourceMapper.toResponseList(infos));
    }

}
