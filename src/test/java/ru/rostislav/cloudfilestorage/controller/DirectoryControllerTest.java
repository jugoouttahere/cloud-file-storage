package ru.rostislav.cloudfilestorage.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.rostislav.cloudfilestorage.integration.MinioIntegrationTest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DirectoryControllerTest extends MinioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @WithMockUser
    @Test
    void shouldReturn404WhenDirectoryDoesNotExist() throws Exception {
        mockMvc.perform(
                        get("/directory")
                                .param("path", "missing/")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Resource not found: missing/"));
    }

    @Test
    void shouldReturn401WhenGettingDirectoryWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        get("/directory")
                                .param("path", "folder/")
                )
                .andExpect(status().isUnauthorized());
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldReturnDirectoryContent() {
        putObject("folder/file1.txt", "Hello");
        putObject("folder/file2.txt", "World");
        putObject("folder/inner/file3.txt", "MinIO");

        mockMvc.perform(
                        get("/directory")
                                .param("path", "folder/")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))

                .andExpect(jsonPath("$[*].name",
                        containsInAnyOrder(
                                "file1.txt",
                                "file2.txt",
                                "inner"
                        )))

                .andExpect(jsonPath("$[*].path",
                        containsInAnyOrder(
                                "folder/",
                                "folder/",
                                "folder/"
                        )))

                .andExpect(jsonPath("$[*].type",
                        containsInAnyOrder(
                                "FILE",
                                "FILE",
                                "DIRECTORY"
                        )));
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldCreateEmptyDirectory() {
        mockMvc.perform(
                        post("/directory")
                                .param("path", "folder/")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("folder"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"))
                .andExpect(jsonPath("$.size").doesNotExist());

        assertTrue(isObjectExist("folder/"));
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldReturn404WhenParentDirectoryDoesNotExist() {
        mockMvc.perform(
                        post("/directory")
                                .param("path", "missing/inner/")
                )
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @WithMockUser
    @Test
    void shouldReturn409WhenDirectoryAlreadyExists() {
        putObject("folder/file.txt", "");

        mockMvc.perform(
                        post("/directory")
                                .param("path", "folder/")
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn401WhenCreatingDirectoryWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        post("/directory")
                                .param("path", "folder/")
                )
                .andExpect(status().isUnauthorized());
    }
}
