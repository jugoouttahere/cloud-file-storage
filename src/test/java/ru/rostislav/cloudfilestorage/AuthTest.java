package ru.rostislav.cloudfilestorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.rostislav.cloudfilestorage.entity.User;
import ru.rostislav.cloudfilestorage.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUser() throws Exception {
        mockMvc.perform(
                post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "RegisterTestUsername",
                                    "password": "123456"
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("RegisterTestUsername"));

        User user = userRepository.findByUsername("RegisterTestUsername").orElseThrow();

        assertEquals("RegisterTestUsername", user.getUsername());

        assertNotEquals("123456", user.getPassword());

        assertEquals(1, userRepository.count());
    }
}
