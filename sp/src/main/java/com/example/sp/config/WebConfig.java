package com.example.sp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = Path.of("uploads").toAbsolutePath().normalize().toUri().toString();
        if (!uploadDir.endsWith("/")) uploadDir += "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir);
    }
}
