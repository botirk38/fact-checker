package com.fact_checker.FactChecker.config;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import okhttp3.OkHttpClient;
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
     * RestTemplate is a synchronous client to perform HTTP requests, exposing a
     * simple, template method API over underlying HTTP client libraries.
     *
     * @return A new instance of RestTemplate.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Creates and configures an OkHttpClient bean.
     * OkHttpClient is a client to perform HTTP requests.
     *
     * @return OkHttpClient object.
     */

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }

    /**
     * Creates an Encoding bean.
     * Encoding is a class that can encode and decode text into a list of integers.
     * @return Encoding object.
     */

    @Bean
    public Encoding encoding() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        return registry.getEncodingForModel(ModelType.TEXT_EMBEDDING_3_SMALL);
    }
}

