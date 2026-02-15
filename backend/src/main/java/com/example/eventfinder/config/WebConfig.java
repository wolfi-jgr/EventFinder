package com.example.eventfinder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors-origins}")
    private String corsOrigins;

    @SuppressWarnings("null")
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Check if wildcard is specified for local development
        if (corsOrigins.contains("*")) {
            registry.addMapping("/api/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowCredentials(true);
        } else {
            registry.addMapping("/api/**")
                    .allowedOrigins(corsOrigins.split(","))
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowCredentials(true);
        }
    }
}
