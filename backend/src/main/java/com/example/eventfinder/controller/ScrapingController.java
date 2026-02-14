package com.example.eventfinder.controller;

import com.example.eventfinder.scraper.ScraperManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Controller for managing web scraping operations
 */
@RestController
@RequestMapping("/api/scraping")
public class ScrapingController {
    
    private final ScraperManager scraperManager;
    
    public ScrapingController(ScraperManager scraperManager) {
        this.scraperManager = scraperManager;
    }
    
    /**
     * POST /api/scraping/run - Run scraping for all enabled sources
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runScraping() {
        Map<String, Object> results = scraperManager.scrapeAllSources();
        return ResponseEntity.ok(results);
    }
    
    /**
     * POST /api/scraping/source/{sourceId} - Run scraping for a specific source
     */
    @PostMapping("/source/{sourceId}")
    public ResponseEntity<Object> scrapeSource(@PathVariable Long sourceId) {
        try {
            ScraperManager.ScrapingResult result = scraperManager.scrapeSourceById(sourceId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * GET /api/scraping/scrapers - Get available scraper types
     */
    @GetMapping("/scrapers")
    public Set<String> getAvailableScrapers() {
        return scraperManager.getAvailableScraperTypes();
    }
}
