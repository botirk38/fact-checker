package com.fact_checker.FactChecker.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration class for rate limiting in the FactChecker application.
 * This class uses the Bucket4j library to implement a token bucket algorithm for rate limiting.
 */
@Configuration
public class RateLimiterConfig {

    /** Maximum number of requests allowed within the refresh period. */
    private static final int MAX_NUMBER_OF_REQUESTS = 200;

    /** Refresh period in minutes. */
    private static final int REFRESH_PERIOD = 1;

    /** Maximum number of tokens that can be accumulated (burst capacity). */
    private static final long OVERDRAFT = 250;

    /**
     * Creates and configures a new Bucket for rate limiting.
     *
     * @return A configured Bucket instance.
     */
    @Bean
    public Bucket createNewBucket() {
        // Define the refill rate: MAX_NUMBER_OF_REQUESTS tokens every REFRESH_PERIOD minute(s)
        Refill refill = Refill.intervally(MAX_NUMBER_OF_REQUESTS, Duration.ofMinutes(REFRESH_PERIOD));

        // Define the overall bandwidth limit, including burst capacity (OVERDRAFT)
        Bandwidth limit = Bandwidth.classic(OVERDRAFT, refill);

        // Build and return the bucket with the defined limit
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
