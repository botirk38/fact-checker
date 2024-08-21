package com.fact_checker.FactChecker.config;

import com.fact_checker.FactChecker.interceptors.RateLimitInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import io.github.bucket4j.Bucket;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String thumbnailLocation;
    private final String videoLocation;
    private final Bucket bucket;
    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);



    public WebConfig(@Value("${thumbnail.upload.path}") String thumbnailLocation, @Value("${video.upload.path}") String videoLocation, Bucket bucket) {
        this.thumbnailLocation = thumbnailLocation;
        this.videoLocation = videoLocation;
        this.bucket = bucket;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        logger.info("Adding resource handlers for thumbnail location: {}", thumbnailLocation);

        registry.addResourceHandler("/thumbnails/**")
                .addResourceLocations("file:" + thumbnailLocation);

        logger.info("Adding resource handlers for video location: {}", videoLocation);
        
        registry.addResourceHandler("/videos-storage/**")
                .addResourceLocations("file:" + videoLocation );

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + "lol");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(bucket));
    }
}