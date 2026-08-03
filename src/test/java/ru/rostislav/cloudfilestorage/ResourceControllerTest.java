package ru.rostislav.cloudfilestorage;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
