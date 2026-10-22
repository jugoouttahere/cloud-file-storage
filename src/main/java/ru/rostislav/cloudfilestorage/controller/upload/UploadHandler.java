package ru.rostislav.cloudfilestorage.controller.upload;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.springframework.stereotype.Component;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;
import ru.rostislav.cloudfilestorage.exception.InvalidPathException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectOperationException;
import ru.rostislav.cloudfilestorage.service.FileStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class UploadHandler {

    private final FileStorageService fileStorageService;

    public List<ResourceResponse> handle(HttpServletRequest request, String path) {
        if (!JakartaServletFileUpload.isMultipartContent(request)) {
            throw new InvalidPathException(path);
        }

        var infos = new ArrayList<ResourceResponse>();
        var upload = new JakartaServletFileUpload<>();

        try {
            var iterator = upload.getItemIterator(request);
            while (iterator.hasNext()) {
                var item = iterator.next();
                if (item.isFormField() || !"object".equals(item.getFieldName())) {
                    continue;
                }
                try (InputStream in = item.getInputStream()) {
                    infos.add(fileStorageService.uploadFile(
                            path, item.getName(), item.getContentType(), in));
                }
            }
        } catch (IOException e) {
            throw new ObjectOperationException(path, "upload", e);
        }
        return infos;
    }
}
