package com.example.eventfinder.config;

import com.example.eventfinder.scraper.ScraperManager;
import com.example.eventfinder.scraper.impl.GenericEventScraper;
import com.example.eventfinder.service.SourceUrlService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to register scrapers and initialize default sources
 */
@Configuration
public class ScraperInitializer {
    
    private final ScraperManager scraperManager;
    private final GenericEventScraper genericScraper;
    private final SourceUrlService sourceUrlService;
    
    public ScraperInitializer(ScraperManager scraperManager,
                              GenericEventScraper genericScraper,
                              SourceUrlService sourceUrlService) {
        this.scraperManager = scraperManager;
        this.genericScraper = genericScraper;
        this.sourceUrlService = sourceUrlService;
    }
    
    @PostConstruct
    public void initialize() {
        // Register scrapers
        scraperManager.registerScraper("generic", genericScraper);
        
        // Initialize default sources
        sourceUrlService.initializeDefaultSources();
    }
    
    @PreDestroy
    public void shutdown() {
        scraperManager.shutdown();
    }
}
