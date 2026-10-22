package ru.rostislav.cloudfilestorage.service;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceResponse;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;
import ru.rostislav.cloudfilestorage.exception.EmptyFileException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectAlreadyExistsException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectNotFoundException;
import ru.rostislav.cloudfilestorage.integration.MinioIntegrationTest;
import ru.rostislav.cloudfilestorage.security.WithMockUserDetails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceTest extends MinioIntegrationTest {

    @Autowired
    private FileStorageService fileStorageService;

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldRenameFile() {
        String text = "Hello MinIO";

        putObject("hello.txt", text);

        fileStorageService.renameFile("hello.txt", "new-hello.txt");

        GetObjectResponse getObjectResponse = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("user-files")
                        .object("user-1-files/new-hello.txt")
                        .build()
        );

        String actual = new String(getObjectResponse.readAllBytes(), StandardCharsets.UTF_8);

        assertFalse(isObjectExist("hello.txt"));
        assertTrue(isObjectExist("new-hello.txt"));
        assertEquals(text, actual);
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldRenameFolder() {
        putObject("old/file1.txt", "");
        putObject("old/file2.txt", "");
        putObject("old/inner/file3.txt", "");

        fileStorageService.renameFolder("old/", "new/");

        assertTrue(isObjectExist("new/file1.txt"));
        assertTrue(isObjectExist("new/file2.txt"));
        assertTrue(isObjectExist("new/inner/file3.txt"));
        assertFalse(isObjectExist("old/file1.txt"));
        assertFalse(isObjectExist("old/file2.txt"));
        assertFalse(isObjectExist("old/inner/file3.txt"));
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldDeleteFile() {
        putObject("hello.txt", "");

        fileStorageService.deleteFile("hello.txt");

        assertFalse(isObjectExist("hello.txt"));
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldUploadFile() {
        String expected = "Hello MinIO";

        ResourceResponse result = fileStorageService.uploadFile(
                "",
                "hello.txt",
                "text/plain",
                new ByteArrayInputStream(expected.getBytes(StandardCharsets.UTF_8))
        );

        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("user-files")
                        .object("user-1-files/hello.txt")
                        .build()
        )) {
            String actual = new String(
                    response.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            assertEquals(expected, actual);
        }

        assertTrue(isObjectExist("hello.txt"));
        assertEquals("hello.txt", result.name());
        assertEquals(11L, result.size());
        assertEquals(ResourceType.FILE, result.type());
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldUploadMultipleFiles() {
        String expectedFile1 = "Hello";
        String expectedFile2 = "World";

        ResourceResponse result1 = fileStorageService.uploadFile(
                "",
                "hello.txt",
                "text/plain",
                new ByteArrayInputStream(
                        expectedFile1.getBytes(StandardCharsets.UTF_8)
                )
        );

        ResourceResponse result2 = fileStorageService.uploadFile(
                "",
                "world.txt",
                "text/plain",
                new ByteArrayInputStream(
                        expectedFile2.getBytes(StandardCharsets.UTF_8)
                )
        );

        try (GetObjectResponse response1 = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("user-files")
                        .object("user-1-files/hello.txt")
                        .build()
        );
             GetObjectResponse response2 = minioClient.getObject(
                     GetObjectArgs.builder()
                             .bucket("user-files")
                             .object("user-1-files/world.txt")
                             .build()
             )) {

            assertEquals(
                    expectedFile1,
                    new String(response1.readAllBytes(), StandardCharsets.UTF_8)
            );

            assertEquals(
                    expectedFile2,
                    new String(response2.readAllBytes(), StandardCharsets.UTF_8)
            );
        }

        assertTrue(isObjectExist("hello.txt"));
        assertTrue(isObjectExist("world.txt"));

        assertEquals(
                new ResourceResponse("", "hello.txt", 5L, ResourceType.FILE),
                result1
        );

        assertEquals(
                new ResourceResponse("", "world.txt", 5L, ResourceType.FILE),
                result2
        );
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldUploadFilesWithNestedDirectories() {
        String expectedFile1 = "Test1";
        String expectedFile2 = "Test2";
        String expectedFile3 = "Test3";

        ResourceResponse result1 = fileStorageService.uploadFile(
                "storage/",
                "test1.txt",
                "text/plain",
                new ByteArrayInputStream(expectedFile1.getBytes(StandardCharsets.UTF_8))
        );

        ResourceResponse result2 = fileStorageService.uploadFile(
                "storage/",
                "folder/test2.txt",
                "text/plain",
                new ByteArrayInputStream(expectedFile2.getBytes(StandardCharsets.UTF_8))
        );

        ResourceResponse result3 = fileStorageService.uploadFile(
                "storage/",
                "folder/inner/test3.txt",
                "text/plain",
                new ByteArrayInputStream(expectedFile3.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(
                new ResourceResponse("storage/", "test1.txt", 5L, ResourceType.FILE),
                result1
        );

        assertEquals(
                new ResourceResponse("storage/folder/", "test2.txt", 5L, ResourceType.FILE),
                result2
        );

        assertEquals(
                new ResourceResponse("storage/folder/inner/", "test3.txt", 5L, ResourceType.FILE),
                result3
        );

        assertTrue(isObjectExist("storage/test1.txt"));
        assertTrue(isObjectExist("storage/folder/test2.txt"));
        assertTrue(isObjectExist("storage/folder/inner/test3.txt"));

        assertEquals(expectedFile1, readObject("storage/test1.txt"));
        assertEquals(expectedFile2, readObject("storage/folder/test2.txt"));
        assertEquals(expectedFile3, readObject("storage/folder/inner/test3.txt"));
    }

    @SneakyThrows
    private String readObject(String path) {
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("user-files")
                        .object("user-1-files/" + path)
                        .build()
        )) {
            return new String(response.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldDownloadFile() {
        String expected = "Hello MinIO";

        putObject("hello.txt", expected);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        fileStorageService.downloadFile("hello.txt")
                .writeTo(outputStream);

        String actual = outputStream.toString(StandardCharsets.UTF_8);

        assertEquals(expected, actual);
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldCreateEmptyFolder() {
        ResourceResponse result = fileStorageService.createEmptyFolder("folder/");

        assertTrue(isObjectExist("folder/"));

        assertEquals("", result.path());
        assertEquals("folder", result.name());
        assertNull(result.size());
        assertEquals(ResourceType.DIRECTORY, result.type());
    }

    @WithMockUserDetails
    @Test
    void shouldThrowWhenUploadingExistingFile() {
        putObject("hello.txt", "");

        assertThrows(
                ObjectAlreadyExistsException.class,
                () -> fileStorageService.uploadFile(
                        "",
                        "hello.txt",
                        "text/plain",
                        new ByteArrayInputStream(
                                "Hello".getBytes(StandardCharsets.UTF_8)
                        )
                )
        );
    }

    @WithMockUserDetails
    @Test
    void shouldThrowWhenUploadingEmptyFile() {
        assertThrows(
                EmptyFileException.class,
                () -> fileStorageService.uploadFile(
                        "",
                        "hello.txt",
                        "text/plain",
                        new ByteArrayInputStream(new byte[0])
                )
        );
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldThrowWhenDownloadingMissingFile() {
        assertThrows(
                ObjectNotFoundException.class,
                () -> fileStorageService.downloadFile("hello.txt")
        );
    }

    @WithMockUserDetails
    @Test
    void shouldThrowWhenDeletingMissingFile() {
        assertThrows(
                ObjectNotFoundException.class,
                () -> fileStorageService.deleteFile("hello.txt")
        );
    }

    @WithMockUserDetails
    @Test
    void shouldThrowWhenRenamingMissingFile() {
        assertThrows(
                ObjectNotFoundException.class,
                () -> fileStorageService.renameFile("hello.txt", "new-hello.txt")
        );
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldThrowWhenRenamingToExistingFile() {
        String text = "Hello MinIO";

        putObject("hello.txt", text);

        assertThrows(
                ObjectAlreadyExistsException.class,
                () -> fileStorageService.renameFile("hello.txt", "hello.txt")
        );
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldThrowWhenCreatingExistingFolder() {
        putObject("folder/file.txt", "");

        assertThrows(
                ObjectAlreadyExistsException.class,
                () -> fileStorageService.createEmptyFolder("folder/")
        );
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldDeleteFolder() {
        putObject("folder/file1.txt", "");
        putObject("folder/file2.txt", "");
        putObject("folder/inner/file3.txt", "");

        fileStorageService.deleteFolder("folder/");

        assertFalse(isObjectExist("folder/file1.txt"));
        assertFalse(isObjectExist("folder/file2.txt"));
        assertFalse(isObjectExist("folder/inner/file3.txt"));
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldDownloadFolder() {
        putObject("folder/file1.txt", "Hello");
        putObject("folder/file2.txt", "World");
        putObject("folder/inner/file3.txt", "MinIO");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        fileStorageService.downloadFolder("folder/")
                .writeTo(outputStream);

        byte[] zipBytes = outputStream.toByteArray();

        try (ZipInputStream zipInputStream =
                     new ZipInputStream(new ByteArrayInputStream(zipBytes))) {

            List<String> entries = new ArrayList<>();
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                entries.add(entry.getName());
            }

            assertTrue(entries.contains("file1.txt"));
            assertTrue(entries.contains("file2.txt"));
            assertTrue(entries.contains("inner/file3.txt"));
        }
    }

    @WithMockUserDetails
    @Test
    void shouldGetDirectoryContent() {
        putObject("folder/file1.txt", "Hello");
        putObject("folder/file2.txt", "World");
        putObject("folder/inner/file3.txt", "MinIO");

        List<ResourceResponse> result = fileStorageService.getDirectoryContent("folder/");

        assertEquals(3, result.size());

        assertTrue(result.contains(
                new ResourceResponse("folder/", "file1.txt", 5L, ResourceType.FILE)
        ));

        assertTrue(result.contains(
                new ResourceResponse("folder/", "file2.txt", 5L, ResourceType.FILE)
        ));

        assertTrue(result.contains(
                new ResourceResponse("folder/", "inner", null, ResourceType.DIRECTORY)
        ));

        assertFalse(result.stream()
                .anyMatch(resource -> resource.name().equals("file3.txt")));
    }

    @WithMockUserDetails
    @Test
    void shouldGetFileInfo() {
        putObject("folder/file.txt", "Hello");

        ResourceResponse result = fileStorageService.getResourceInfo("folder/file.txt");

        assertEquals(
                new ResourceResponse(
                        "folder/",
                        "file.txt",
                        5L,
                        ResourceType.FILE
                ),
                result
        );
    }

    @WithMockUserDetails
    @Test
    void shouldGetFolderInfo() {
        putObject("folder/file.txt", "Hello");

        ResourceResponse result = fileStorageService.getResourceInfo("folder/");

        assertEquals(
                new ResourceResponse(
                        "",
                        "folder",
                        null,
                        ResourceType.DIRECTORY
                ),
                result
        );
    }

    @WithMockUserDetails
    @Test
    void shouldSearchResources() {
        putObject("documents/report.txt", "Report");
        putObject("documents/photo.jpg", "Photo");
        putObject("documents/archive/report-old.txt", "Old report");

        List<ResourceResponse> result = fileStorageService.searchResource("report");

        assertEquals(2, result.size());

        assertTrue(result.contains(
                new ResourceResponse(
                        "documents/",
                        "report.txt",
                        6L,
                        ResourceType.FILE
                )
        ));

        assertTrue(result.contains(
                new ResourceResponse(
                        "documents/archive/",
                        "report-old.txt",
                        10L,
                        ResourceType.FILE
                )
        ));
    }

    @WithMockUserDetails
    @Test
    void shouldSearchDirectory() {
        putFolder("documents/archive/");
        putObject("documents/archive/file.txt", "File");

        List<ResourceResponse> result = fileStorageService.searchResource("archive");

        assertTrue(result.contains(
                new ResourceResponse(
                        "documents/",
                        "archive",
                        null,
                        ResourceType.DIRECTORY
                )
        ));
    }

    @WithMockUserDetails
    @Test
    void shouldSearchResourcesIgnoringCase() {
        putObject("documents/MyReport.txt", "Report");

        List<ResourceResponse> result = fileStorageService.searchResource("MYREPORT");

        assertTrue(result.contains(
                new ResourceResponse(
                        "documents/",
                        "MyReport.txt",
                        6L,
                        ResourceType.FILE
                )
        ));
    }

    @WithMockUserDetails
    @Test
    void shouldMoveFile() {
        putObject("folder/old.txt", "Hello");

        ResourceResponse result =
                fileStorageService.moveResource("folder/old.txt", "folder/new.txt");

        assertEquals(
                new ResourceResponse(
                        "folder/",
                        "new.txt",
                        5L,
                        ResourceType.FILE
                ),
                result
        );

        assertFalse(isObjectExist("folder/old.txt"));
        assertTrue(isObjectExist("folder/new.txt"));
    }

    @WithMockUserDetails
    @Test
    void shouldMoveFolder() {
        putObject("old-folder/file1.txt", "Hello");
        putObject("old-folder/inner/file2.txt", "World");

        ResourceResponse result =
                fileStorageService.moveResource("old-folder/", "new-folder/");

        assertEquals(
                new ResourceResponse(
                        "",
                        "new-folder",
                        null,
                        ResourceType.DIRECTORY
                ),
                result
        );

        assertFalse(isObjectExist("old-folder/file1.txt"));
        assertFalse(isObjectExist("old-folder/inner/file2.txt"));

        assertTrue(isObjectExist("new-folder/file1.txt"));
        assertTrue(isObjectExist("new-folder/inner/file2.txt"));
    }

    @WithMockUserDetails
    @Test
    void shouldThrowWhenMovingFileToExistingPath() {
        putObject("folder/old.txt", "Hello");
        putObject("folder/existing.txt", "World");

        assertThrows(
                ObjectAlreadyExistsException.class,
                () -> fileStorageService.moveResource(
                        "folder/old.txt",
                        "folder/existing.txt"
                )
        );

        assertTrue(isObjectExist("folder/old.txt"));
        assertTrue(isObjectExist("folder/existing.txt"));
    }

    @WithMockUserDetails
    @Test
    void shouldThrowWhenMovingFolderToExistingPath() {
        putObject("old-folder/file.txt", "Hello");
        putObject("existing-folder/file.txt", "World");

        assertThrows(
                ObjectAlreadyExistsException.class,
                () -> fileStorageService.moveResource(
                        "old-folder/",
                        "existing-folder/"
                )
        );

        assertTrue(isObjectExist("old-folder/file.txt"));
        assertTrue(isObjectExist("existing-folder/file.txt"));
    }

    @WithMockUserDetails
    @Test
    void shouldAllowFileAndFolderWithSameName() {
        putObject("documents/report", "Hello");
        putFolder("documents/report/");

        ResourceResponse fileInfo =
                fileStorageService.getResourceInfo("documents/report");

        ResourceResponse folderInfo =
                fileStorageService.getResourceInfo("documents/report/");

        assertEquals(
                new ResourceResponse(
                        "documents/",
                        "report",
                        5L,
                        ResourceType.FILE
                ),
                fileInfo
        );

        assertEquals(
                new ResourceResponse(
                        "documents/",
                        "report",
                        null,
                        ResourceType.DIRECTORY
                ),
                folderInfo
        );
    }


}