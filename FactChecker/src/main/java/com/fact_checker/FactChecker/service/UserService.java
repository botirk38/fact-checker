package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.model.User;
import com.fact_checker.FactChecker.repository.UserRepository;
import com.fact_checker.FactChecker.exceptions.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Optional;
import java.util.Set;
/**
 * Service class for managing user-related operations.
 * This class handles user registration and retrieval.
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);



    /**
      * Constructs a new UserService with the necessary dependencies.
      *
      * @param userRepository The repository for User entities.
      * @param passwordEncoder The encoder for hashing passwords.
      */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
      * Registers a new user in the system.
      *
      * @param username The desired username for the new user.
      * @param email The email address of the new user.
      * @param password The password for the new user (will be encoded before storage).
      * @param fullName The full name of the new user.
      * @return The newly created and saved User entity.
      * @throws IllegalArgumentException If any of the input parameters are null or empty.
      * @throws UserAlreadyExistsException If a user with the given username or email already exists.
      */
    public User registerUser(String username, String password, String email, String fullName) {
        // Input validation
        if (!StringUtils.hasText(username) || !StringUtils.hasText(email) ||
                !StringUtils.hasText(password) || !StringUtils.hasText(fullName)) {
            throw new IllegalArgumentException("All fields must be non-empty");
        }

        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // Create and save new user

        // Set roles
        Set<String> roles = Set.of("USER");

        String encodedPassword = passwordEncoder.encode(password);

        User user = new User(username, email, encodedPassword, fullName, roles);
        logger.info("Registering new user: username={}, email={}, roles={}", username, email, roles);

        return userRepository.save(user);
    }

    /**
     * Finds a user by their username or email.
     *
     * @param usernameOrEmail The username or email of the user to find.
     * @return An Optional containing the found User, or an empty Optional if no user was found.
     */
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));
    }


    public String generateUniquePassword() {
        int length = 16;
        boolean useLetters = true;
        boolean useNumbers = true;
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }


}
