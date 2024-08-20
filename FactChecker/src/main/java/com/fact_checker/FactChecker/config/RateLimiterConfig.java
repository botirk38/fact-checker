package com.fact_checker.FactChecker.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public Bucket createNewBucket() {
        long overdraft = 50;
        Refill refill = Refill.intervally(20, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(overdraft, refill);
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
