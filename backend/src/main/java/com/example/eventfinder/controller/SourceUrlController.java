package com.example.eventfinder.controller;

import com.example.eventfinder.model.SourceUrl;
import com.example.eventfinder.service.SourceUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
public class SourceUrlController {
    
    private final SourceUrlService sourceUrlService;
    
    public SourceUrlController(SourceUrlService sourceUrlService) {
        this.sourceUrlService = sourceUrlService;
    }
    
    /**
     * GET /api/sources - Get all source URLs
     */
    @GetMapping
    public List<SourceUrl> getAllSources() {
        return sourceUrlService.getAllSourceUrls();
    }
    
    /**
     * GET /api/sources/enabled - Get only enabled sources
     */
    @GetMapping("/enabled")
    public List<SourceUrl> getEnabledSources() {
        return sourceUrlService.getEnabledSourceUrls();
    }
    
    /**
     * GET /api/sources/{id} - Get source by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SourceUrl> getSourceById(@PathVariable Long id) {
        return sourceUrlService.getSourceUrlById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/sources/category/{category} - Get sources by category
     */
    @GetMapping("/category/{category}")
    public List<SourceUrl> getSourcesByCategory(@PathVariable String category) {
        return sourceUrlService.getSourceUrlsByCategory(category);
    }
    
    /**
     * POST /api/sources - Create a new source URL
     */
    @PostMapping
    public ResponseEntity<SourceUrl> createSource(@RequestBody SourceUrl sourceUrl) {
        SourceUrl created = sourceUrlService.createSourceUrl(sourceUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * PUT /api/sources/{id} - Update a source URL
     */
    @PutMapping("/{id}")
    public ResponseEntity<SourceUrl> updateSource(
            @PathVariable Long id, 
            @RequestBody SourceUrl sourceUrl) {
        try {
            SourceUrl updated = sourceUrlService.updateSourceUrl(id, sourceUrl);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * DELETE /api/sources/{id} - Delete a source URL
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable Long id) {
        try {
            sourceUrlService.deleteSourceUrl(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * POST /api/sources/{id}/toggle - Toggle enabled status
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<SourceUrl> toggleEnabled(@PathVariable Long id) {
        try {
            SourceUrl updated = sourceUrlService.toggleEnabled(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * POST /api/sources/initialize - Initialize default system sources
     */
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeDefaults() {
        sourceUrlService.initializeDefaultSources();
        return ResponseEntity.ok("Default sources initialized");
    }
    
    /**
     * POST /api/sources/fix-scrapers - Fix scraper types for existing sources
     */
    @PostMapping("/fix-scrapers")
    public ResponseEntity<String> fixScraperTypes() {
        int fixed = sourceUrlService.fixScraperTypes();
        return ResponseEntity.ok("Fixed " + fixed + " sources to use 'generic' scraper");
    }
}
