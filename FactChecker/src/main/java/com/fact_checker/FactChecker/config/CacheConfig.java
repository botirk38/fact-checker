package com.fact_checker.FactChecker.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration class for caching in the FactChecker application.
 * This class sets up a Caffeine-based cache manager for efficient data storage and retrieval.
 */
@Configuration
public class CacheConfig {
    /** Maximum number of entries in the cache. */
    
    private static final int MAX_SIZE = 1000;

    /** Maximum number of entries in the cache. */
    private static final int TIME_TO_LIVE = 5;

    /**
     * Creates and configures a CacheManager bean.
     *
     * @return A configured CaffeineCacheManager instance.
     */
    @Bean
    public CacheManager cacheManager() {
        // Initialize CaffeineCacheManager with specific cache names
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("videos", "audioExtractions", "transcriptions", "thumbnails");

        // Set the Caffeine cache specifications
        cacheManager.setCaffeine(caffeineCacheBuilder());

        // Enable asynchronous cache mode for improved performance
        cacheManager.setAsyncCacheMode(true);

        return cacheManager;
    }

    /**
     * Defines the Caffeine cache specifications.
     *
     * @return A Caffeine instance with defined cache properties.
     */
    Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)  // Set maximum number of entries the cache may contain
                .expireAfterWrite(TIME_TO_LIVE, TimeUnit.HOURS);  // Set the duration after which entries should be automatically removed
    }
}
