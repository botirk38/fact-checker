package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.model.User;
import com.fact_checker.FactChecker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.Set;

/**
 * Custom OAuth2 user service that extends the default OAuth2 user service.
 * This service is responsible for loading user details from an OAuth2 provider
 * and creating or updating local user records based on the OAuth2 user information.
 */
@Service
public class CustomOAuth2Service extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * Constructs a new CustomOAuth2Service with the specified UserRepository.
     *
     * @param userRepository The repository used to manage User entities.
     */
    public CustomOAuth2Service(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the user from the OAuth2 provider and creates or updates the local user record.
     *
     * @param userRequest The OAuth2UserRequest containing information about the authentication request.
     * @return An OAuth2User representing the authenticated user.
     * @throws OAuth2AuthenticationException If an error occurs during authentication.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extract user details from OAuth2User
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        // Check if user exists, if not, create a new user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(email, name));

        // Return a new DefaultOAuth2User with the user's details
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oauth2User.getAttributes(),
                "email"
        );
    }

    /**
     * Creates a new User entity based on the OAuth2 user information.
     *
     * @param email The email address of the user.
     * @param name The full name of the user.
     * @return The newly created and persisted User entity.
     */
    private User createNewUser(String email, String name) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFullName(name);
        newUser.setUsername(email); // Note: You might want to generate a unique username
        newUser.setRoles(Set.of("USER"));
        return userRepository.save(newUser);
    }
}
