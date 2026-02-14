package com.example.eventfinder.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents a user-defined URL source for scraping events.
 * Users can add their favorite event websites to personalize their experience.
 */
@Entity
@Table(name = "source_urls")
public class SourceUrl {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String url;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    /**
     * Category of events this source provides (Music, Sports, Culture, etc.)
     */
    private String category;
    
    /**
     * Domain of the source (e.g., "eventbrite.com")
     */
    private String domain;
    
    /**
     * Type of scraper to use for this source
     * (e.g., "eventbrite", "falter", "generic")
     */
    @Column(name = "scraper_type")
    private String scraperType;
    
    /**
     * Whether this source is enabled for scraping
     */
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;
    
    /**
     * Whether this is a system-provided source or user-added
     */
    @Column(name = "is_system")
    private Boolean isSystem = false;
    
    /**
     * Last time this source was successfully scraped
     */
    @Column(name = "last_scraped_at")
    private Instant lastScrapedAt;
    
    /**
     * Last time scraping this source failed
     */
    @Column(name = "last_error_at")
    private Instant lastErrorAt;
    
    /**
     * Error message from last failed scrape attempt
     */
    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;
    
    /**
     * Number of consecutive scraping failures
     */
    @Column(name = "failure_count")
    private Integer failureCount = 0;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    // Constructors
    
    public SourceUrl() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public String getScraperType() {
        return scraperType;
    }
    
    public void setScraperType(String scraperType) {
        this.scraperType = scraperType;
    }
    
    public Boolean getIsEnabled() {
        return isEnabled;
    }
    
    public void setIsEnabled(Boolean enabled) {
        isEnabled = enabled;
    }
    
    public Boolean getIsSystem() {
        return isSystem;
    }
    
    public void setIsSystem(Boolean system) {
        isSystem = system;
    }
    
    public Instant getLastScrapedAt() {
        return lastScrapedAt;
    }
    
    public void setLastScrapedAt(Instant lastScrapedAt) {
        this.lastScrapedAt = lastScrapedAt;
    }
    
    public Instant getLastErrorAt() {
        return lastErrorAt;
    }
    
    public void setLastErrorAt(Instant lastErrorAt) {
        this.lastErrorAt = lastErrorAt;
    }
    
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
    
    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }
    
    public Integer getFailureCount() {
        return failureCount;
    }
    
    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Mark scraping as successful
     */
    public void markScrapingSuccess() {
        this.lastScrapedAt = Instant.now();
        this.failureCount = 0;
        this.lastErrorAt = null;
        this.lastErrorMessage = null;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Mark scraping as failed
     */
    public void markScrapingFailure(String errorMessage) {
        this.lastErrorAt = Instant.now();
        this.lastErrorMessage = errorMessage;
        this.failureCount = (this.failureCount != null ? this.failureCount : 0) + 1;
        this.updatedAt = Instant.now();
        
        // Auto-disable after 5 consecutive failures
        if (this.failureCount >= 5) {
            this.isEnabled = false;
        }
    }
}
