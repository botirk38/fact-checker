package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.model.User;
import com.fact_checker.FactChecker.repository.UserRepository;
import com.fact_checker.FactChecker.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registerUser_ValidInput_ReturnsUser() {
        // Arrange
        String username = "testuser";
        String email = "test@example.com";
        String password = "password";
        String fullName = "Test User";
        String encodedPassword = "encodedPassword";
        Set<String> expectedRoles = Set.of("USER");

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(username, savedUser.getUsername());
            assertEquals(email, savedUser.getEmail());
            assertEquals(encodedPassword, savedUser.getPassword());
            assertEquals(fullName, savedUser.getFullName());
            assertEquals(expectedRoles, savedUser.getRoles());
            return savedUser;
        });

        // Act
        User result = userService.registerUser(username, password, email, fullName);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(encodedPassword, result.getPassword());
        assertEquals(fullName, result.getFullName());
        assertEquals(expectedRoles, result.getRoles());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_UsernameExists_ThrowsUserAlreadyExistsException() {
        // Arrange
        String username = "existinguser";
        String email = "test@example.com";
        String password = "password";
        String fullName = "Test User";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () ->
                userService.registerUser(username, password, email, fullName)
        );
    }

    @Test
    void registerUser_EmailExists_ThrowsUserAlreadyExistsException() {
        // Arrange
        String username = "newuser";
        String email = "existing@example.com";
        String password = "password";
        String fullName = "Test User";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () ->
                userService.registerUser(username, password, email, fullName)
        );
    }

    @Test
    void registerUser_EmptyInput_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser("", "password", "test@example.com", "Test User")
        );
        assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser("testuser", "", "test@example.com", "Test User")
        );
        assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser("testuser", "password", "", "Test User")
        );
        assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser("testuser", "password", "test@example.com", "")
        );
    }

    @Test
    void findByUsernameOrEmail_UserExists_ReturnsUser() {
        // Arrange
        String username = "testuser";
        User user = new User();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByUsernameOrEmail(username);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void findByUsernameOrEmail_EmailExists_ReturnsUser() {
        // Arrange
        String email = "test@example.com";
        User user = new User();
        when(userRepository.findByUsername(email)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByUsernameOrEmail(email);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void findByUsernameOrEmail_UserNotFound_ReturnsEmptyOptional() {
        // Arrange
        String usernameOrEmail = "nonexistent";
        when(userRepository.findByUsername(usernameOrEmail)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(usernameOrEmail)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsernameOrEmail(usernameOrEmail);

        // Assert
        assertFalse(result.isPresent());
    }
}
