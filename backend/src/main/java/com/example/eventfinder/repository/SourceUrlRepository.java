package com.example.eventfinder.repository;

import com.example.eventfinder.model.SourceUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceUrlRepository extends JpaRepository<SourceUrl, Long> {
    
    /**
     * Find all enabled source URLs
     */
    List<SourceUrl> findByIsEnabledTrue();
    
    /**
     * Find source URLs by scraper type
     */
    List<SourceUrl> findByScraperType(String scraperType);
    
    /**
     * Find source URLs by category
     */
    List<SourceUrl> findByCategory(String category);
    
    /**
     * Find source URLs by domain
     */
    List<SourceUrl> findByDomain(String domain);
    
    /**
     * Find all system-provided sources
     */
    List<SourceUrl> findByIsSystemTrue();
    
    /**
     * Find all user-added sources
     */
    List<SourceUrl> findByIsSystemFalse();
}
