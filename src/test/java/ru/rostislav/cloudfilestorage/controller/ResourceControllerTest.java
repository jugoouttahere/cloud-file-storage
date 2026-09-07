package ru.rostislav.cloudfilestorage.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.rostislav.cloudfilestorage.integration.MinioIntegrationTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ResourceControllerTest extends MinioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldReturnResourceInfo() {
        putObject("hello.txt", "Hello MinIO");
        mockMvc.perform(
                        get("/resource")
                                .param("path", "hello.txt")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("hello.txt"))
                .andExpect(jsonPath("$.size").value(11))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldReturn404WhenResourceDoesNotExist() {
        mockMvc.perform(
                        get("/resource")
                                .param("path", "missing.txt")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found: missing.txt"));
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldUploadFiles() {
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "hello.txt",
                "text/plain",
                "Hello".getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "world.txt",
                "text/plain",
                "World".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(
                        multipart("/resource")
                                .file(file1)
                                .file(file2)
                                .param("path", "storage/")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("hello.txt"))
                .andExpect(jsonPath("$[0].size").value(5))
                .andExpect(jsonPath("$[0].type").value("FILE"))
                .andExpect(jsonPath("$[1].name").value("world.txt"))
                .andExpect(jsonPath("$[1].size").value(5))
                .andExpect(jsonPath("$[1].type").value("FILE"))
                .andExpect(jsonPath("$[0].path").value("storage/"))
                .andExpect(jsonPath("$[1].path").value("storage/"));
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldDeleteResource() {
        putObject("hello.txt", "Hello MinIO");

        mockMvc.perform(
                        delete("/resource")
                                .param("path", "hello.txt")
                )
                .andExpect(status().isNoContent());

        assertFalse(isObjectExist("hello.txt"));
    }

    @WithMockUser
    @Test
    void shouldReturn404WhenDeletingMissingResource() throws Exception {
        mockMvc.perform(
                        delete("/resource")
                                .param("path", "missing.txt")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Resource not found: missing.txt"));
    }

    @Test
    void shouldReturn401WhenDeletingResourceWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        delete("/resource")
                                .param("path", "hello.txt")
                )
                .andExpect(status().isUnauthorized());
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldDownloadFile() {
        String expected = "Hello MinIO";

        putObject("hello.txt", expected);

        mockMvc.perform(
                        get("/resource/download")
                                .param("path", "hello.txt")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(
                        expected.getBytes(StandardCharsets.UTF_8)
                ));
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldDownloadFolderAsZip() {
        putObject("folder/file1.txt", "Hello");
        putObject("folder/file2.txt", "World");
        putObject("folder/inner/file3.txt", "MinIO");

        MvcResult result = mockMvc.perform(
                        get("/resource/download")
                                .param("path", "folder/")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();

        byte[] zipBytes = result.getResponse().getContentAsByteArray();

        Map<String, String> files = new HashMap<>();

        try (ZipInputStream zipInputStream =
                     new ZipInputStream(new ByteArrayInputStream(zipBytes))) {

            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                String content = new String(
                        zipInputStream.readAllBytes(),
                        StandardCharsets.UTF_8
                );

                files.put(entry.getName(), content);
            }
        }

        assertEquals(3, files.size());

        assertEquals("Hello", files.get("file1.txt"));
        assertEquals("World", files.get("file2.txt"));
        assertEquals("MinIO", files.get("inner/file3.txt"));
    }

    @WithMockUser
    @Test
    void shouldReturn404WhenDownloadingMissingResource() throws Exception {
        mockMvc.perform(
                        get("/resource/download")
                                .param("path", "missing.txt")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Resource not found: missing.txt"));
    }

    @WithMockUser
    @Test
    void shouldReturn404WhenDownloadingMissingFolder() throws Exception {
        mockMvc.perform(
                        get("/resource/download")
                                .param("path", "missing/")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401WhenDownloadingResourceWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        get("/resource/download")
                                .param("path", "hello.txt")
                )
                .andExpect(status().isUnauthorized());
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldMoveFile() {
        putObject("hello.txt", "Hello");
        mockMvc.perform(
                        post("/resource/move")
                                .param("from", "hello.txt")
                                .param("to", "new-hello.txt")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("new-hello.txt"))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.type").value("FILE"));

        assertFalse(isObjectExist("hello.txt"));
        assertTrue(isObjectExist("new-hello.txt"));
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldMoveFolder() {
        putObject("folder/file1.txt", "Hello");
        putObject("folder/file2.txt", "World");
        putObject("folder/inner/file3.txt", "MinIO");

        mockMvc.perform(
                        post("/resource/move")
                                .param("from", "folder/")
                                .param("to", "new-folder/")
                )
                .andExpect(status().isOk());

        assertTrue(isObjectExist("new-folder/file1.txt"));
        assertTrue(isObjectExist("new-folder/file2.txt"));
        assertTrue(isObjectExist("new-folder/inner/file3.txt"));

        assertFalse(isObjectExist("folder/file1.txt"));
        assertFalse(isObjectExist("folder/file2.txt"));
        assertFalse(isObjectExist("folder/inner/file3.txt"));
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldReturn404WhenMovingMissingFile() {
        mockMvc.perform(
                        post("/resource/move")
                                .param("from", "missing.txt")
                                .param("to", "new-file.txt")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Resource not found: missing.txt"));
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldReturn404WhenMovingMissingFolder() {
        mockMvc.perform(
                        post("/resource/move")
                                .param("from", "missing/")
                                .param("to", "new-folder/")
                )
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldReturn409WhenMovingFileToExistingPath() {
        putObject("hello.txt", "Hello");
        putObject("new-hello.txt", "World");

        mockMvc.perform(
                        post("/resource/move")
                                .param("from", "hello.txt")
                                .param("to", "new-hello.txt")
                )
                .andExpect(status().isConflict());
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldReturn409WhenMovingFolderToExistingPath() {
        putObject("folder/file.txt", "Hello");
        putObject("new-folder/file.txt", "World");

        mockMvc.perform(
                        post("/resource/move")
                                .param("from", "folder/")
                                .param("to", "new-folder/")
                )
                .andExpect(status().isConflict());
    }

    @SneakyThrows
    @Test
    void shouldReturn401WhenMovingResourceWithoutAuthentication() {
        mockMvc.perform(
                        post("/resource/move")
                                .param("from", "hello.txt")
                                .param("to", "new-hello.txt")
                )
                .andExpect(status().isUnauthorized());
    }
}
