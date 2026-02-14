package com.example.eventfinder.service;

import com.example.eventfinder.model.SourceUrl;
import com.example.eventfinder.repository.SourceUrlRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SourceUrlService {
    
    private final SourceUrlRepository sourceUrlRepository;
    
    public SourceUrlService(SourceUrlRepository sourceUrlRepository) {
        this.sourceUrlRepository = sourceUrlRepository;
    }
    
    /**
     * Get all source URLs
     */
    public List<SourceUrl> getAllSourceUrls() {
        return sourceUrlRepository.findAll();
    }
    
    /**
     * Get only enabled source URLs
     */
    public List<SourceUrl> getEnabledSourceUrls() {
        return sourceUrlRepository.findByIsEnabledTrue();
    }
    
    /**
     * Get source URL by ID
     */
    public Optional<SourceUrl> getSourceUrlById(Long id) {
        return sourceUrlRepository.findById(id);
    }
    
    /**
     * Get source URLs by category
     */
    public List<SourceUrl> getSourceUrlsByCategory(String category) {
        return sourceUrlRepository.findByCategory(category);
    }
    
    /**
     * Get source URLs by scraper type
     */
    public List<SourceUrl> getSourceUrlsByScraperType(String scraperType) {
        return sourceUrlRepository.findByScraperType(scraperType);
    }
    
    /**
     * Create a new source URL
     */
    public SourceUrl createSourceUrl(SourceUrl sourceUrl) {
        sourceUrl.setCreatedAt(Instant.now());
        sourceUrl.setUpdatedAt(Instant.now());
        
        // Extract domain from URL if not provided
        if (sourceUrl.getDomain() == null && sourceUrl.getUrl() != null) {
            sourceUrl.setDomain(extractDomain(sourceUrl.getUrl()));
        }
        
        return sourceUrlRepository.save(sourceUrl);
    }
    
    /**
     * Update an existing source URL
     */
    public SourceUrl updateSourceUrl(Long id, SourceUrl updatedSourceUrl) {
        return sourceUrlRepository.findById(id)
            .map(existing -> {
                existing.setName(updatedSourceUrl.getName());
                existing.setUrl(updatedSourceUrl.getUrl());
                existing.setDescription(updatedSourceUrl.getDescription());
                existing.setCategory(updatedSourceUrl.getCategory());
                existing.setScraperType(updatedSourceUrl.getScraperType());
                existing.setIsEnabled(updatedSourceUrl.getIsEnabled());
                existing.setUpdatedAt(Instant.now());
                
                if (updatedSourceUrl.getDomain() != null) {
                    existing.setDomain(updatedSourceUrl.getDomain());
                }
                
                return sourceUrlRepository.save(existing);
            })
            .orElseThrow(() -> new RuntimeException("SourceUrl not found with id: " + id));
    }
    
    /**
     * Delete a source URL
     */
    public void deleteSourceUrl(Long id) {
        sourceUrlRepository.deleteById(id);
    }
    
    /**
     * Toggle enabled status
     */
    public SourceUrl toggleEnabled(Long id) {
        return sourceUrlRepository.findById(id)
            .map(sourceUrl -> {
                sourceUrl.setIsEnabled(!sourceUrl.getIsEnabled());
                sourceUrl.setUpdatedAt(Instant.now());
                return sourceUrlRepository.save(sourceUrl);
            })
            .orElseThrow(() -> new RuntimeException("SourceUrl not found with id: " + id));
    }
    
    /**
     * Mark scraping success
     */
    public void markScrapingSuccess(Long id) {
        sourceUrlRepository.findById(id).ifPresent(sourceUrl -> {
            sourceUrl.markScrapingSuccess();
            sourceUrlRepository.save(sourceUrl);
        });
    }
    
    /**
     * Mark scraping failure
     */
    public void markScrapingFailure(Long id, String errorMessage) {
        sourceUrlRepository.findById(id).ifPresent(sourceUrl -> {
            sourceUrl.markScrapingFailure(errorMessage);
            sourceUrlRepository.save(sourceUrl);
        });
    }
    
    /**
     * Initialize default system sources if none exist
     */
    public void initializeDefaultSources() {
        if (sourceUrlRepository.findByIsSystemTrue().isEmpty()) {
            // Add default Vienna event sources
            createDefaultSource(
                "Falter Events Vienna",
                "https://www.falter.at/events",
                "General event calendar for Vienna",
                "All",
                "falter.at",
                "generic"
            );
            
            // createDefaultSource(
            //     "Wien.info Events",
            //     "https://www.wien.info/en/music-stage-shows",
            //     "Official Vienna tourism event listings",
            //     "Culture",
            //     "wien.info",
            //     "generic"
            // );
        }
    }
    
    /**
     * Fix scraper types for existing sources (sets all non-generic to generic)
     */
    public int fixScraperTypes() {
        List<SourceUrl> allSources = sourceUrlRepository.findAll();
        int fixed = 0;
        
        for (SourceUrl source : allSources) {
            if (!"generic".equals(source.getScraperType())) {
                source.setScraperType("generic");
                source.setUpdatedAt(Instant.now());
                sourceUrlRepository.save(source);
                fixed++;
            }
        }
        
        return fixed;
    }
    
    private void createDefaultSource(String name, String url, String description, 
                                     String category, String domain, String scraperType) {
        SourceUrl source = new SourceUrl();
        source.setName(name);
        source.setUrl(url);
        source.setDescription(description);
        source.setCategory(category);
        source.setDomain(domain);
        source.setScraperType(scraperType);
        source.setIsSystem(true);
        source.setIsEnabled(true);
        sourceUrlRepository.save(source);
    }
    
    private String extractDomain(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            return parsedUrl.getHost();
        } catch (Exception e) {
            return null;
        }
    }
}
