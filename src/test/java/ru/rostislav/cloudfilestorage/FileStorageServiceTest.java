package ru.rostislav.cloudfilestorage;

import io.minio.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceInfo;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;
import ru.rostislav.cloudfilestorage.exception.minio.EmptyFileException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectAlreadyExistsException;
import ru.rostislav.cloudfilestorage.exception.minio.ObjectNotFoundException;
import ru.rostislav.cloudfilestorage.service.FileStorageService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceTest extends MinioIntegrationTest {

    @Autowired
    private FileStorageService fileStorageService;

    @SneakyThrows
    @Test
    void shouldRenameFile() {
        String text = "Hello MinIO";

        putObject("hello.txt", text);

        fileStorageService.renameFile("hello.txt", "new-hello.txt");

        GetObjectResponse getObjectResponse = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("new-hello.txt")
                        .build()
        );

        String actual = new String(getObjectResponse.readAllBytes(), StandardCharsets.UTF_8);

        assertFalse(isObjectExist("hello.txt"));
        assertTrue(isObjectExist("new-hello.txt"));
        assertEquals(text, actual);
    }

    @SneakyThrows
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
    @Test
    void shouldDeleteFile() {
        putObject("hello.txt", "");

        fileStorageService.deleteFile("hello.txt");

        assertFalse(isObjectExist("hello.txt"));
    }

    @SneakyThrows
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
                        .object("hello.txt")
                        .build()
        );

        String actual = new String(getObjectResponse.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(isObjectExist("hello.txt"));
        assertEquals(expected, actual);
    }

    @SneakyThrows
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
                        .object("hello.txt")
                        .build()
        );

        GetObjectResponse responseFile2 = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("world.txt")
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
                        .object("storage/test1.txt")
                        .build()
        );

        GetObjectResponse responseFile2 = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("storage/folder/test2.txt")
                        .build()
        );

        GetObjectResponse responseFile3 = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("storage/folder/inner/test3.txt")
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
    @Test
    void shouldCreateEmptyFolder() {
        fileStorageService.createEmptyFolder("folder/");

        StatObjectResponse statObject = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket("cloud-storage")
                        .object("folder/")
                        .build()
        );

        assertEquals(0, statObject.size());
    }

    @SneakyThrows
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
                () -> fileStorageService.uploadFiles("hello.txt", List.of(file))
        );
    }

    @SneakyThrows
    @Test
    void shouldThrowWhenDownloadingMissingFile() {
        assertThrows(
                ObjectNotFoundException.class,
                () -> fileStorageService.downloadFile("hello.txt")
        );
    }

    @Test
    void shouldThrowWhenDeletingMissingFile() {
        assertThrows(
                ObjectNotFoundException.class,
                () -> fileStorageService.deleteFile("hello.txt")
        );
    }

    @Test
    void shouldThrowWhenRenamingMissingFile() {
        assertThrows(
                ObjectNotFoundException.class,
                () -> fileStorageService.renameFile("hello.txt", "new-hello.txt")
        );
    }

    @SneakyThrows
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
    @Test
    void shouldThrowWhenCreatingExistingFolder() {
        putObject("folder/", "");

        assertThrows(
                ObjectAlreadyExistsException.class,
                () -> fileStorageService.createEmptyFolder("folder/")
        );
    }
}