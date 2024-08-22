package com.fact_checker.FactChecker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAI API settings in the FactChecker application.
 * This class manages the API key and URL required for interacting with OpenAI services.
 */
@Configuration
@Getter
@Setter
public class OpenAIConfig {

    /**
     * The API key for authenticating requests to the OpenAI API.
     * This value is injected from the application's configuration properties.
     */
    @Value("${openai.api.key}")
    private String apiKey;

    /**
     * The base URL for the OpenAI API.
     * This value is injected from the application's configuration properties.
     */
    @Value("${openai.api.url}")
    private String apiUrl;
}

