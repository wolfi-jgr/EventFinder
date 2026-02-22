package com.example.eventfinder.config;

import com.example.eventfinder.scraper.ScraperManager;
import com.example.eventfinder.scraper.impl.GenericEventScraper;
import com.example.eventfinder.service.SourceUrlService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to register scrapers and initialize default sources
 */
@Configuration
public class ScraperInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ScraperInitializer.class);
    
    private final ScraperManager scraperManager;
    private final GenericEventScraper genericEventScraper;
    private final SourceUrlService sourceUrlService;
    
    public ScraperInitializer(ScraperManager scraperManager,
                              GenericEventScraper genericEventScraper,
                              SourceUrlService sourceUrlService) {
        this.scraperManager = scraperManager;
        this.genericEventScraper = genericEventScraper;
        this.sourceUrlService = sourceUrlService;
    }
    
    @PostConstruct
    public void initialize() {
        // Register legacy scrapers
        scraperManager.registerScraper("generic", genericEventScraper);
        
        // Note: GenericScraper (rule-based) is used directly by ScrapeOrchestrationService
        // via scrapeWithRule() method, not registered with ScraperManager
        logger.info("Scraper initialization complete");
        logger.info("- Legacy GenericEventScraper registered with ScraperManager");
        logger.info("- Rule-based GenericScraper available for ScrapeOrchestrationService");
        
        // Initialize default sources
        sourceUrlService.initializeDefaultSources();
    }
    
    @PreDestroy
    public void shutdown() {
        scraperManager.shutdown();
    }
}
