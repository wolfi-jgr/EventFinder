package com.example.eventfinder;

import com.example.eventfinder.config.ScrapeRulesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ScrapeRulesProperties.class)
public class EventFinderApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventFinderApplication.class, args);
    }
}
