package com.example.eventfinder.controller;

import com.example.eventfinder.service.ScrapeOrchestrationService;
import com.example.eventfinder.service.ScrapeRuleSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing web scraping operations using rule-based system
 */
@RestController
@RequestMapping("/api/scraping")
public class ScrapingController {
    
    private final ScrapeOrchestrationService orchestrationService;
    private final ScrapeRuleSyncService scrapeRuleSyncService;
    
    public ScrapingController(
            ScrapeOrchestrationService orchestrationService,
            ScrapeRuleSyncService scrapeRuleSyncService) {
        this.orchestrationService = orchestrationService;
        this.scrapeRuleSyncService = scrapeRuleSyncService;
    }
    
    // ========== RULE-BASED SCRAPING ENDPOINTS ==========
    
    /**
     * POST /api/scraping/rules/run - Run scraping for all enabled sites using rules
     */
    @PostMapping("/rules/run")
    public ResponseEntity<Map<String, Object>> runRuleBasedScraping() {
        Map<String, Object> results = orchestrationService.scrapeAll();
        return ResponseEntity.ok(results);
    }

    /**
     * POST /api/scraping/rules/sync - Reload and sync scrape-rules.yml to database
     */
    @PostMapping("/rules/sync")
    public ResponseEntity<Map<String, Object>> syncRules() {
        Map<String, Object> result = scrapeRuleSyncService.syncNow();
        return ResponseEntity.ok(result);
    }
    
    /**
     * POST /api/scraping/rules/site/{siteName} - Run scraping for a specific site
     */
    @PostMapping("/rules/site/{siteName}")
    public ResponseEntity<Object> scrapeSite(@PathVariable String siteName) {
        return executeScrapeSite(siteName);
    }

    /**
     * POST /api/scraping/rules/site/** - Backward compatible endpoint when siteName contains '/'
     */
    @PostMapping("/rules/site/**")
    public ResponseEntity<Object> scrapeSiteWildcard(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String marker = "/api/scraping/rules/site/";
        int markerIndex = uri.indexOf(marker);

        if (markerIndex < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid site path"));
        }

        String rawSiteName = uri.substring(markerIndex + marker.length());
        String decodedSiteName = URLDecoder.decode(rawSiteName, StandardCharsets.UTF_8);
        return executeScrapeSite(decodedSiteName);
    }

    /**
     * POST /api/scraping/rules/site?siteName=... - Preferred endpoint for names containing '/'
     */
    @PostMapping("/rules/site")
    public ResponseEntity<Object> scrapeSiteByQuery(@RequestParam String siteName) {
        return executeScrapeSite(siteName);
    }

    private ResponseEntity<Object> executeScrapeSite(String siteName) {
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
    @DeleteMapping("/cache")
    public ResponseEntity<?> clearCache(@RequestParam String siteName){
        String decodedSiteName = URLDecoder.decode(siteName, StandardCharsets.UTF_8);
        orchestrationService.clearCache(decodedSiteName);
        return ResponseEntity.ok(Map.of("message", "Cache cleared for " + decodedSiteName));
    }

    /**
     * DELETE /api/scraping/events/site?siteName=... - Delete all scraped events for one site
     */
    @DeleteMapping("/events/site")
    public ResponseEntity<Map<String, Object>> clearEventsForSite(@RequestParam String siteName) {
        Map<String, Object> result = orchestrationService.clearEventsBySiteName(siteName);
        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
