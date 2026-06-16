package com.example.eventfinder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import com.example.eventfinder.web.AdminAuthInterceptor;

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

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            // Protect scraping-related endpoints and other admin actions
            registry.addInterceptor(new AdminAuthInterceptor())
                    .addPathPatterns("/api/scraping/**");
        }
}
