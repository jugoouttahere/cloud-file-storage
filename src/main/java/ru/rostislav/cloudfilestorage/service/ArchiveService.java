package ru.rostislav.cloudfilestorage.service;

import org.springframework.stereotype.Service;
import ru.rostislav.cloudfilestorage.dto.ArchiveEntry;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ArchiveService {

    public InputStream createZip(List<ArchiveEntry> entries) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

            for (ArchiveEntry entry : entries) {
                ZipEntry zipEntry = new ZipEntry(entry.path());
                zipOutputStream.putNextEntry(zipEntry);

                if (entry.type() == ResourceType.FILE) {
                    try (InputStream inputStream = entry.content()) {
                        inputStream.transferTo(zipOutputStream);
                    }
                }
                zipOutputStream.closeEntry();
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to create ZIP archive", e);
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
