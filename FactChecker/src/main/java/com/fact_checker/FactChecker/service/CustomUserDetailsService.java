package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.model.User;
import com.fact_checker.FactChecker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * CustomUserDetailsService is a service class that implements the UserDetailsService interface
 * from Spring Security. It provides custom user authentication and authorization functionality.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    /**
     * Constructs a new CustomUserDetailsService with the specified UserRepository.
     *
     * @param userRepository The repository used to access user data.
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by their username.
     *
     * This method is used by Spring Security to load user-specific data during authentication.
     * It retrieves the user from the database based on the provided username.
     *
     * @param username The username of the user to load.
     * @return A UserDetails object containing the user's information.
     * @throws UsernameNotFoundException If no user is found with the given username.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        logger.debug("Loaded user: {}", username);
        logger.debug("Stored password hash: {}", user.getPassword());

        // Deencode the password from the stored hash.




        return user;
    }
}
