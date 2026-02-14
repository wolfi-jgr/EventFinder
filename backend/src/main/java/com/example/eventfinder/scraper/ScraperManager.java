package com.example.eventfinder.scraper;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.SourceUrl;
import com.example.eventfinder.service.EventService;
import com.example.eventfinder.service.SourceUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Central manager for coordinating web scrapers with rate limiting and concurrency control.
 */
@Service
public class ScraperManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ScraperManager.class);
    
    private final ScraperConfig config;
    private final EventService eventService;
    private final SourceUrlService sourceUrlService;
    private final Map<String, BaseWebScraper> scrapers = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    
    public ScraperManager(ScraperConfig config, 
                          EventService eventService, 
                          SourceUrlService sourceUrlService) {
        this.config = config;
        this.eventService = eventService;
        this.sourceUrlService = sourceUrlService;
        this.executorService = Executors.newFixedThreadPool(config.getMaxConcurrentScrapers());
    }
    
    /**
     * Register a scraper implementation
     */
    public void registerScraper(String scraperType, BaseWebScraper scraper) {
        scrapers.put(scraperType, scraper);
        logger.info("Registered scraper: {} for type: {}", scraper.getScraperName(), scraperType);
    }
    
    /**
     * Scrape all enabled sources
     */
    public Map<String, Object> scrapeAllSources() {
        if (!config.isEnabled()) {
            logger.warn("Scraping is disabled in configuration");
            return Map.of("status", "disabled", "message", "Scraping is disabled");
        }
        
        List<SourceUrl> sources = sourceUrlService.getEnabledSourceUrls();
        logger.info("Starting scraping for {} enabled sources", sources.size());
        
        Map<String, Object> results = new ConcurrentHashMap<>();
        List<Future<ScrapingResult>> futures = new ArrayList<>();
        
        for (SourceUrl source : sources) {
            Future<ScrapingResult> future = executorService.submit(() -> scrapeSource(source));
            futures.add(future);
        }
        
        int totalEvents = 0;
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Future<ScrapingResult> future : futures) {
            try {
                ScrapingResult result = future.get(60, TimeUnit.SECONDS); // 60 second timeout per source
                totalEvents += result.eventCount;
                
                if (result.success) {
                    successCount++;
                } else {
                    failureCount++;
                    errors.add(result.sourceName + ": " + result.errorMessage);
                }
            } catch (TimeoutException e) {
                failureCount++;
                errors.add("Timeout waiting for scraping to complete");
                logger.error("Scraping timeout", e);
            } catch (Exception e) {
                failureCount++;
                errors.add("Unexpected error: " + e.getMessage());
                logger.error("Error collecting scraping result", e);
            }
        }
        
        results.put("totalEvents", totalEvents);
        results.put("successCount", successCount);
        results.put("failureCount", failureCount);
        results.put("errors", errors);
        
        logger.info("Scraping completed: {} events from {} sources ({} successful, {} failed)", 
            totalEvents, sources.size(), successCount, failureCount);
        
        return results;
    }
    
    /**
     * Scrape a specific source
     */
    public ScrapingResult scrapeSource(SourceUrl source) {
        ScrapingResult result = new ScrapingResult();
        result.sourceName = source.getName();
        
        try {
            logger.info("Scraping source: {} ({})", source.getName(), source.getUrl());
            
            BaseWebScraper scraper = scrapers.get(source.getScraperType());
            
            // Fall back to generic scraper if specific scraper not found
            if (scraper == null) {
                logger.warn("No scraper registered for type '{}', falling back to generic scraper", 
                    source.getScraperType());
                scraper = scrapers.get("generic");
                
                if (scraper == null) {
                    throw new IllegalStateException("Generic scraper not available");
                }
            }
            
            List<Event> events = scraper.scrapeEvents(source.getUrl());
            
            if (events != null && !events.isEmpty()) {
                List<Event> savedEvents = eventService.saveAllEvents(events);
                result.eventCount = savedEvents.size();
                logger.info("Successfully scraped {} events from {}", savedEvents.size(), source.getName());
            } else {
                logger.warn("No events found from {}", source.getName());
            }
            
            result.success = true;
            sourceUrlService.markScrapingSuccess(source.getId());
            
        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getMessage();
            logger.error("Failed to scrape source: " + source.getName(), e);
            sourceUrlService.markScrapingFailure(source.getId(), e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Scrape a specific source by ID
     */
    public ScrapingResult scrapeSourceById(Long sourceId) {
        return sourceUrlService.getSourceUrlById(sourceId)
            .map(this::scrapeSource)
            .orElseThrow(() -> new IllegalArgumentException("Source not found with id: " + sourceId));
    }
    
    /**
     * Get registered scraper types
     */
    public Set<String> getAvailableScraperTypes() {
        return scrapers.keySet();
    }
    
    /**
     * Result of a scraping operation
     */
    public static class ScrapingResult {
        public boolean success;
        public int eventCount;
        public String sourceName;
        public String errorMessage;
    }
    
    /**
     * Shutdown executor service on application shutdown
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
