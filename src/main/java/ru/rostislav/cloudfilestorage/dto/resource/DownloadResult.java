package ru.rostislav.cloudfilestorage.dto.resource;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record DownloadResult(
        String filename,
        StreamingResponseBody body
) {
}
