package com.example.eventfinder.controller;

import com.example.eventfinder.service.ScrapeOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing web scraping operations using rule-based system
 */
@RestController
@RequestMapping("/api/scraping")
public class ScrapingController {
    
    private final ScrapeOrchestrationService orchestrationService;
    
    public ScrapingController(ScrapeOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
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
