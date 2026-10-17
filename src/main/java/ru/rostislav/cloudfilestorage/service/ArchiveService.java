package ru.rostislav.cloudfilestorage.service;

import org.springframework.stereotype.Service;
import ru.rostislav.cloudfilestorage.dto.ArchiveEntry;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ArchiveService {

    public void writeToZip(List<ArchiveEntry> entries, OutputStream out) {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {

            for (ArchiveEntry entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.path()));

                if (entry.type() == ResourceType.FILE) {
                    try (InputStream in = entry.content().get()) {
                        in.transferTo(zip);
                    }
                }
                zip.closeEntry();
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to write to ZIP archive", e);
        }
    }
}
