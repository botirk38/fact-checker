package com.fact_checker.FactChecker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String thumbnailLocation;
    private final String videoLocation;

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);


    public WebConfig(@Value("${thumbnail.upload.path}") String thumbnailLocation, @Value("${video.upload.path}") String videoLocation) {
        this.thumbnailLocation = thumbnailLocation;
        this.videoLocation = videoLocation;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/thumbnails/**")
                .addResourceLocations("file:" + thumbnailLocation + "/");


        registry.addResourceHandler("/videos/**")
                .addResourceLocations("file:" + videoLocation + "/");
    }
}