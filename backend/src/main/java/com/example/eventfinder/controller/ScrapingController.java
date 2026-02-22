package com.example.eventfinder.controller;

import com.example.eventfinder.scraper.ScraperManager;
import com.example.eventfinder.service.ScrapeOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for managing web scraping operations
 */
@RestController
@RequestMapping("/api/scraping")
public class ScrapingController {
    
    private final ScraperManager scraperManager;
    private final ScrapeOrchestrationService orchestrationService;
    
    public ScrapingController(ScraperManager scraperManager, 
                             ScrapeOrchestrationService orchestrationService) {
        this.scraperManager = scraperManager;
        this.orchestrationService = orchestrationService;
    }
    
    /**
     * POST /api/scraping/run - Run scraping for all enabled sources (legacy)
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runScraping() {
        Map<String, Object> results = scraperManager.scrapeAllSources();
        return ResponseEntity.ok(results);
    }
    
    /**
     * POST /api/scraping/source/{sourceId} - Run scraping for a specific source (legacy)
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
     * GET /api/scraping/scrapers - Get available scraper types (legacy)
     */
    @GetMapping("/scrapers")
    public Set<String> getAvailableScrapers() {
        return scraperManager.getAvailableScraperTypes();
    }
    
    // ========== NEW RULE-BASED SCRAPING ENDPOINTS ==========
    
    /**
     * POST /api/scraping/rules/run - Run scraping for all enabled sites using rules
     */
    @PostMapping("/rules/run")
    public ResponseEntity<Map<String, Object>> runRuleBasedScraping() {
        Map<String, Object> results = orchestrationService.scrapeAll();
        return ResponseEntity.ok(results);
    }
    
    /**
     * POST /api/scraping/rules/site/{siteName} - Run scraping for a specific site
     */
    @PostMapping("/rules/site/{siteName}")
    public ResponseEntity<Object> scrapeSite(@PathVariable String siteName) {
        try {
            ScrapeOrchestrationService.ScrapeSiteResult result = 
                orchestrationService.scrapeBySiteName(siteName);
            
            if (result.error != null) {
                return ResponseEntity.badRequest().body(Map.of("error", result.error));
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * GET /api/scraping/rules/status - Get status of all configured sites
     */
    @GetMapping("/rules/status")
    public ResponseEntity<List<Map<String, Object>>> getSiteStatus() {
        List<Map<String, Object>> status = orchestrationService.getSiteStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * DELETE /api/scraping/cache/{siteName} - Clear cached HTML for a site
     */
    @DeleteMapping("/cache/{siteName}")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String siteName) {
        orchestrationService.clearCache(siteName);
        return ResponseEntity.ok(Map.of("message", "Cache cleared for " + siteName));
    }
}
