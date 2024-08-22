package com.fact_checker.FactChecker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for defining beans in the FactChecker application.
 * This class is responsible for creating and configuring beans that can be used
 * throughout the application via dependency injection.
 */
@Configuration
public class BeanConfig {

    /**
     * Creates and configures a RestTemplate bean.
     *
     * RestTemplate is a synchronous client to perform HTTP requests, exposing a
     * simple, template method API over underlying HTTP client libraries.
     *
     * @return A new instance of RestTemplate.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
