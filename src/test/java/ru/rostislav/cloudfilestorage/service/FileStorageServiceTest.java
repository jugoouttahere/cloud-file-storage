package ru.rostislav.cloudfilestorage.service;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;
import ru.rostislav.cloudfilestorage.exception.EmptyFileException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectAlreadyExistsException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectNotFoundException;
import ru.rostislav.cloudfilestorage.integration.MinioIntegrationTest;
import ru.rostislav.cloudfilestorage.security.WithMockUserDetails;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
                        .bucket("cloud-storage")
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

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                expected.getBytes(StandardCharsets.UTF_8)
        );

        fileStorageService.uploadFiles("", List.of(file));

        GetObjectResponse getObjectResponse = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("user-1-files/hello.txt")
                        .build()
        );

        String actual = new String(getObjectResponse.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(isObjectExist("hello.txt"));
        assertEquals(expected, actual);
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldUploadMultipleFiles() {
        String expectedFile1 = "Hello";
        String expectedFile2 = "World";

        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "hello.txt",
                "text/plain",
                expectedFile1.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "world.txt",
                "text/plain",
                expectedFile2.getBytes(StandardCharsets.UTF_8)
        );

        List<ResourceInfo> result = fileStorageService.uploadFiles(
                "",
                List.of(file1, file2)
        );

        GetObjectResponse responseFile1 = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("user-1-files/hello.txt")
                        .build()
        );

        GetObjectResponse responseFile2 = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("user-1-files/world.txt")
                        .build()
        );

        String actualFile1 = new String(responseFile1.readAllBytes(), StandardCharsets.UTF_8);
        String actualFile2 = new String(responseFile2.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(2, result.size());

        assertTrue(isObjectExist("hello.txt"));
        assertTrue(isObjectExist("world.txt"));

        assertEquals(expectedFile1, actualFile1);
        assertEquals(expectedFile2, actualFile2);

        ResourceInfo test1 = result.stream()
                .filter(info -> info.name().equals("hello.txt"))
                .findFirst()
                .orElseThrow();

        assertEquals("/", test1.path());
        assertEquals("hello.txt", test1.name());
        assertEquals(5, test1.size());
        assertEquals(ResourceType.FILE, test1.type());

        ResourceInfo test2 = result.stream()
                .filter(info -> info.name().equals("world.txt"))
                .findFirst()
                .orElseThrow();

        assertEquals("/", test2.path());
        assertEquals("world.txt", test2.name());
        assertEquals(5, test2.size());
        assertEquals(ResourceType.FILE, test2.type());
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldUploadFilesWithNestedDirectories() {
        String expectedFile1 = "Test1";
        String expectedFile2 = "Test2";
        String expectedFile3 = "Test3";

        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "test1.txt",
                "text/plain",
                expectedFile1.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "folder/test2.txt",
                "text/plain",
                expectedFile2.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile file3 = new MockMultipartFile(
                "files",
                "folder/inner/test3.txt",
                "text/plain",
                expectedFile3.getBytes(StandardCharsets.UTF_8)
        );

        List<ResourceInfo> result = fileStorageService.uploadFiles(
                "storage/",
                List.of(file1, file2, file3)
        );

        GetObjectResponse responseFile1 = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("user-1-files/storage/test1.txt")
                        .build()
        );

        GetObjectResponse responseFile2 = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("user-1-files/storage/folder/test2.txt")
                        .build()
        );

        GetObjectResponse responseFile3 = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("user-1-files/storage/folder/inner/test3.txt")
                        .build()
        );

        String actualFile1 = new String(responseFile1.readAllBytes(), StandardCharsets.UTF_8);
        String actualFile2 = new String(responseFile2.readAllBytes(), StandardCharsets.UTF_8);
        String actualFile3 = new String(responseFile3.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(3, result.size());

        assertTrue(isObjectExist("storage/test1.txt"));
        assertTrue(isObjectExist("storage/folder/test2.txt"));
        assertTrue(isObjectExist("storage/folder/inner/test3.txt"));

        assertEquals(expectedFile1, actualFile1);
        assertEquals(expectedFile2, actualFile2);
        assertEquals(expectedFile3, actualFile3);

        ResourceInfo test1 = result.stream()
                .filter(info -> info.name().equals("test1.txt"))
                .findFirst()
                .orElseThrow();

        assertEquals("storage/", test1.path());
        assertEquals("test1.txt", test1.name());
        assertEquals(5, test1.size());
        assertEquals(ResourceType.FILE, test1.type());

        ResourceInfo test2 = result.stream()
                .filter(info -> info.name().equals("test2.txt"))
                .findFirst()
                .orElseThrow();

        assertEquals("storage/folder/", test2.path());
        assertEquals("test2.txt", test2.name());
        assertEquals(5, test2.size());
        assertEquals(ResourceType.FILE, test2.type());

        ResourceInfo test3 = result.stream()
                .filter(info -> info.name().equals("test3.txt"))
                .findFirst()
                .orElseThrow();

        assertEquals("storage/folder/inner/", test3.path());
        assertEquals("test3.txt", test3.name());
        assertEquals(5, test3.size());
        assertEquals(ResourceType.FILE, test3.type());
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldDownloadFile() {
        String expected = "Hello MinIO";

        putObject("hello.txt", expected);

        String actual;
        try (InputStream downloadFile = fileStorageService.downloadFile("hello.txt")) {
            actual = new String(downloadFile.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(expected, actual);
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldCreateEmptyFolder() {
        ResourceInfo result = fileStorageService.createEmptyFolder("folder/");

        assertTrue(isObjectExist("folder/"));

        assertEquals("/", result.path());
        assertEquals("folder", result.name());
        assertNull(result.size());
        assertEquals(ResourceType.DIRECTORY, result.type());
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldThrowWhenUploadingExistingFile() {
        putObject("hello.txt", "");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                "Hello".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(
                ObjectAlreadyExistsException.class,
                () -> fileStorageService.uploadFiles("", List.of(file))
        );
    }

    @SneakyThrows
    @WithMockUserDetails
    @Test
    void shouldThrowWhenUploadingEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                new byte[0]
        );

        assertThrows(
                EmptyFileException.class,
                () -> fileStorageService.uploadFiles("", List.of(file))
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

        try (InputStream inputStream = fileStorageService.downloadFolder("folder/")) {
            byte[] zipBytes = inputStream.readAllBytes();

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
    }

    @WithMockUserDetails
    @Test
    void shouldGetDirectoryContent() {
        putObject("folder/file1.txt", "Hello");
        putObject("folder/file2.txt", "World");
        putObject("folder/inner/file3.txt", "MinIO");

        List<ResourceInfo> result = fileStorageService.getDirectoryContent("folder/");

        assertEquals(3, result.size());

        assertTrue(result.contains(
                new ResourceInfo("folder/", "file1.txt", 5L, ResourceType.FILE)
        ));

        assertTrue(result.contains(
                new ResourceInfo("folder/", "file2.txt", 5L, ResourceType.FILE)
        ));

        assertTrue(result.contains(
                new ResourceInfo("folder/", "inner", null, ResourceType.DIRECTORY)
        ));

        assertFalse(result.stream()
                .anyMatch(resource -> resource.name().equals("file3.txt")));
    }

    @WithMockUserDetails
    @Test
    void shouldGetFileInfo() {
        putObject("folder/file.txt", "Hello");

        ResourceInfo result = fileStorageService.getResourceInfo("folder/file.txt");

        assertEquals(
                new ResourceInfo(
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

        ResourceInfo result = fileStorageService.getResourceInfo("folder/");

        assertEquals(
                new ResourceInfo(
                        "/",
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

        List<ResourceInfo> result = fileStorageService.searchResource("report");

        assertEquals(2, result.size());

        assertTrue(result.contains(
                new ResourceInfo(
                        "documents/",
                        "report.txt",
                        6L,
                        ResourceType.FILE
                )
        ));

        assertTrue(result.contains(
                new ResourceInfo(
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

        List<ResourceInfo> result = fileStorageService.searchResource("archive");

        assertTrue(result.contains(
                new ResourceInfo(
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

        List<ResourceInfo> result = fileStorageService.searchResource("MYREPORT");

        assertTrue(result.contains(
                new ResourceInfo(
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

        ResourceInfo result =
                fileStorageService.moveResource("folder/old.txt", "folder/new.txt");

        assertEquals(
                new ResourceInfo(
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

        ResourceInfo result =
                fileStorageService.moveResource("old-folder/", "new-folder/");

        assertEquals(
                new ResourceInfo(
                        "/",
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

        ResourceInfo fileInfo =
                fileStorageService.getResourceInfo("documents/report");

        ResourceInfo folderInfo =
                fileStorageService.getResourceInfo("documents/report/");

        assertEquals(
                new ResourceInfo(
                        "documents/",
                        "report",
                        5L,
                        ResourceType.FILE
                ),
                fileInfo
        );

        assertEquals(
                new ResourceInfo(
                        "documents/",
                        "report",
                        null,
                        ResourceType.DIRECTORY
                ),
                folderInfo
        );
    }


}