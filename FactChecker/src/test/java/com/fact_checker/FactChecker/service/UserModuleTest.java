package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.model.User;
import com.fact_checker.FactChecker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest
@AutoConfigureMockMvc
public class UserModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll(); // Clear out any existing data before each test
    }

    @Test
    void testRegisterUser() throws Exception {
        // Register a new user
        String userJson = "{\"username\":\"testuser\",\"password\":\"password123\",\"email\":\"testuser@example.com\"}";

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User registered successfully with ID:")));

        // Verify user exists in the repository
        User user = userRepository.findByUsername("testuser").orElse(null);
        assert user != null;
        assert passwordEncoder.matches("password123", user.getPassword());
    }


    @Test
    void testLoginUser() throws Exception {
        // Register a new user
        User user = new User("testuser", passwordEncoder.encode("password123"), "testuser@example.com");
        userRepository.save(user);

        // Attempt to log in
        String loginJson = "{\"username\":\"testuser\",\"password\":\"password123\"}";

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().string("User logged in successfully: testuser"));
    }

    @Test
    void testLoginUserWithInvalidCredentials() throws Exception {
        // Attempt to log in with invalid credentials
        String loginJson = "{\"username\":\"nonexistentuser\",\"password\":\"wrongpassword\"}";

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid Username or Password"));
    }

    @Test
    void testFindUserByUsername() throws Exception {
        // Register a new user
        User user = new User("testuser", passwordEncoder.encode("password123"), "testuser@example.com");
        userRepository.save(user);

        // Attempt to find the user by username
        mockMvc.perform(get("/api/users/testuser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("testuser@example.com")))
                .andExpect(jsonPath("$.id", is(notNullValue())));
    }


    @Test
    void testFindNonexistentUserByUsername() throws Exception {
        // Attempt to find a non-existent user by username
        mockMvc.perform(get("/api/users/nonexistentuser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
