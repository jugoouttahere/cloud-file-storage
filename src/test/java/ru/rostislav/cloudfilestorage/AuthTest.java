package ru.rostislav.cloudfilestorage;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.rostislav.cloudfilestorage.entity.User;
import ru.rostislav.cloudfilestorage.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
                                            "password": "12345678"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("RegisterTestUsername"));

        User user = userRepository.findByUsername("RegisterTestUsername").orElseThrow();
        assertEquals("RegisterTestUsername", user.getUsername());
        assertTrue(passwordEncoder.matches("12345678", user.getPassword()));
        assertEquals(1, userRepository.count());
    }

    @Test
    void shouldLoginGetCurrentUserAndLogout() throws Exception {
        String encodedPassword = passwordEncoder.encode("12345678");
        userRepository.save(new User("TestUsername", encodedPassword));

        MvcResult mvcResult = mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "TestUsername",
                                            "password": "12345678"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("TestUsername"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) mvcResult.getRequest().getSession();
        assertNotNull(session);

        mockMvc.perform(
                get("/api/user/me")
                        .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("TestUsername"));

        mockMvc.perform(
                post("/api/auth/sign-out")
                        .session(session)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/user/me")
                                .session(session)
                )
                .andExpect(status().isUnauthorized());
    }
}
