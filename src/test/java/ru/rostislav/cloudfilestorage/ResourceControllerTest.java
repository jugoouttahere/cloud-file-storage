package ru.rostislav.cloudfilestorage;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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
                .andExpect(jsonPath("$.message").value("File with name:missing.txt not found."));
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
                        .value("File with name:missing.txt not found."));
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
        putObject("folder/", "");

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
                        .value("File with name:missing.txt not found."));
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
}
