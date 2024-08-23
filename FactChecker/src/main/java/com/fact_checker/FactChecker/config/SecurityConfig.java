package com.fact_checker.FactChecker.config;

import com.fact_checker.FactChecker.handlers.OAuth2AuthenticationSuccessHandler;
import com.fact_checker.FactChecker.repository.UserRepository;
import com.fact_checker.FactChecker.service.CustomUserDetailsService;
import com.fact_checker.FactChecker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Configuration class for Spring Security.
 * This class sets up the security configuration for the application,
 * including authentication, authorization, and OAuth2 login.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Constructor for SecurityConfig.
     *
     * @param customUserDetailsService The custom user details service for authentication.
     */
    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Configures the authentication failure handler.
     *
     * @return An AuthenticationFailureHandler that redirects to the login page with an error message.
     */
    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            logger.warn("Failure reason: {}", exception.getMessage());

            // Redirect to login page with error message
            response.sendRedirect("/login?error=true&message=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8));
        };
    }

    /**
     * Configures the security filter chain.
     *
     * @param http The HttpSecurity to modify.
     * @param userRepository The user repository.
     * @param userService The user service.
     * @return A SecurityFilterChain with the configured security settings.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository userRepository, UserService userService) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/", "/css/**").permitAll()
                        .requestMatchers("/home", "/fact-check-video").hasAuthority("ROLE_USER")
                        .requestMatchers("/", "/css/**", "/images/**", "/signup", "/login").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin((form) -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .permitAll()
                        .failureHandler(authenticationFailureHandler())
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .successHandler(new OAuth2AuthenticationSuccessHandler(userService))
                )
                .logout(LogoutConfigurer::permitAll)
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            logger.error("Access denied: {}", accessDeniedException.getMessage());
                            response.sendRedirect("/access-denied");
                        })
                );


        return http.build();
    }

    /**
     * Configures the password encoder.
     *
     * @return A BCryptPasswordEncoder for encoding passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the authentication manager.
     *
     * @param passwordEncoder The password encoder to use.
     * @return An AuthenticationManager configured with a DaoAuthenticationProvider.
     */
    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authProvider);
    }
}
