package com.fact_checker.FactChecker.handlers;

import com.fact_checker.FactChecker.service.CustomUserDetailsService;
import com.fact_checker.FactChecker.service.UserService;
import com.fact_checker.FactChecker.model.User;
import com.fact_checker.FactChecker.exceptions.UserAlreadyExistsException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Handler for successful OAuth2 authentication.
 * This class is responsible for processing the user information after a successful OAuth2 login,
 * creating or retrieving the user in the system, and setting up the user session.
 */
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    private final UserService userService;

    /**
     * Constructs a new OAuth2AuthenticationSuccessHandler with the specified UserService.
     *
     * @param userService The service used to manage user-related operations.
     */
    public OAuth2AuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    /**
     * Handles the authentication success event.
     * This method is called when a user successfully authenticates via OAuth2.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @param authentication The authentication object containing user details.
     * @throws IOException If an I/O error occurs.
     * @throws ServletException If a servlet error occurs.
     */
    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = getEmail(oauth2User);
        String name = getName(oauth2User);

        try {
            User user = findOrCreateUser(email, name, "GOOGLE");
            createSession(request, user);
            logger.info("User authenticated successfully. Email: {}", email);

            // Create a new authentication with updated authorities
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities()
            );

            // Set the new authentication in the security context
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            String targetUrl = determineTargetUrl(request, response, newAuth);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            logger.error("Error during OAuth2 authentication success handling", e);
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    /**
     * Extracts the email from the OAuth2User object.
     *
     * @param oauth2User The OAuth2User object.
     * @return The user's email.
     * @throws IllegalArgumentException If the email is not provided by the OAuth2 provider.
     */
    private String getEmail(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email not provided by OAuth2 provider");
        }
        return email;
    }

    /**
     * Extracts the name from the OAuth2User object.
     *
     * @param oauth2User The OAuth2User object.
     * @return The user's name, or "Unknown" if not provided.
     */
    private String getName(OAuth2User oauth2User) {
        return Optional.ofNullable(oauth2User.getAttribute("name"))
                .map(Object::toString)
                .orElse("Unknown");
    }

    /**
     * Finds an existing user or creates a new one based on the OAuth2 information.
     *
     * @param email The user's email.
     * @param name The user's name.
     * @return The User object.
     */
    private User findOrCreateUser(String email, String name, String provider) {
        return userService.findByEmail(email)
                .orElseGet(() -> createNewUser(email, name, provider));
    }

    /**
     * Creates a new user with the provided email and name.
     *
     * @param email The user's email.
     * @param name The user's name.
     * @return The newly created User object.
     */
    private User createNewUser(String email, String name, String provider) {
        String username = generateUniqueUsername(email);
        String password = userService.generateUniquePassword();
        try {
            return userService.registerUser(username, password, email, name, provider);
        } catch (UserAlreadyExistsException e) {
            logger.warn("Unexpected user already exists error", e);
            throw new RuntimeException("Unexpected user already exists error", e);
        }
    }

    /**
     * Generates a unique username based on the user's email.
     *
     * @param email The user's email.
     * @return A unique username.
     */
    private String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int suffix = 1;
        while (userService.findByEmail(username).isPresent()) {
            username = baseUsername + suffix++;
        }
        return username;
    }

    /**
     * Creates a session for the authenticated user.
     *
     * @param request The HTTP request.
     * @param user The authenticated User object.
     */
    private void createSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession();
        session.setAttribute("user", user);
        logger.debug("Session created for user: {}", user.getEmail());
    }

    /**
     * Determines the target URL to redirect to after successful authentication.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @param authentication The authentication object.
     * @return The target URL.
     */
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        return "/home";
    }


}
