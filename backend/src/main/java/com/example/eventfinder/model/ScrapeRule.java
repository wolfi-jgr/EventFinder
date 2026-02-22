package com.example.eventfinder.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Configuration rules for scraping a specific website.
 * Each site has one ScrapeRule with CSS selectors and parsing patterns.
 */
@Entity
@Table(name = "scrape_rules")
public class ScrapeRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String siteName; // e.g., "theloft.at"

    @Column(nullable = false)
    private String baseUrl; // e.g., "https://www.theloft.at/programm/"

    @Column(length = 1000)
    private String eventListSelector; // CSS selector for event list container

    @Column(length = 1000)
    private String eventItemSelector; // CSS selector for individual events

    @Column(length = 500)
    private String titleSelector; // How to extract title

    @Column(length = 500)
    private String dateSelector; // How to extract date/time

    @Column(length = 500)
    private String priceSelector; // How to extract price

    @Column(length = 500)
    private String venueSelector; // How to extract venue/location

    @Column(length = 500)
    private String linkSelector; // How to extract event detail link

    @Column(length = 500)
    private String imageSelector; // How to extract image

    @Column(length = 500)
    private String descriptionSelector; // How to extract description

    // Date/time parsing config
    @Column(length = 100)
    private String dateFormat; // e.g., "dd.MM.yyyy HH:mm"

    @Column(length = 100)
    private String dateLocale; // e.g., "de-DE" for German

    // Extraction mode
    @Column(length = 50)
    private String extractionMode; // "CSS_SELECTOR", "REGEX", "AI_FALLBACK"

    // For regex-based extraction
    @Column(length = 1000)
    private String eventTextPattern; // Regex pattern to extract from raw text

    // Site metadata
    @Column(length = 100)
    private String category; // Default category for events from this site

    @Column(length = 100)
    private String organizer; // Default organizer

    private Boolean requiresDetailPageFetch; // If true, fetch individual event pages

    @Column(length = 500)
    private String detailPageSelector; // Selector to get to detail page

    // AI fallback config
    private Boolean aiEnabled; // Use AI if generic parsing fails

    @Column(length = 500)
    private String aiPrompt; // Custom prompt for AI extraction

    // Scraping config
    private Integer rateLimitMs; // Custom rate limit for this site

    private Boolean enabled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(length = 2000)
    private String notes; // Admin notes about this site

    // Constructors
    public ScrapeRule() {
        this.enabled = true;
        this.aiEnabled = false;
        this.requiresDetailPageFetch = false;
        this.extractionMode = "CSS_SELECTOR";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEventListSelector() {
        return eventListSelector;
    }

    public void setEventListSelector(String eventListSelector) {
        this.eventListSelector = eventListSelector;
    }

    public String getEventItemSelector() {
        return eventItemSelector;
    }

    public void setEventItemSelector(String eventItemSelector) {
        this.eventItemSelector = eventItemSelector;
    }

    public String getTitleSelector() {
        return titleSelector;
    }

    public void setTitleSelector(String titleSelector) {
        this.titleSelector = titleSelector;
    }

    public String getDateSelector() {
        return dateSelector;
    }

    public void setDateSelector(String dateSelector) {
        this.dateSelector = dateSelector;
    }

    public String getPriceSelector() {
        return priceSelector;
    }

    public void setPriceSelector(String priceSelector) {
        this.priceSelector = priceSelector;
    }

    public String getVenueSelector() {
        return venueSelector;
    }

    public void setVenueSelector(String venueSelector) {
        this.venueSelector = venueSelector;
    }

    public String getLinkSelector() {
        return linkSelector;
    }

    public void setLinkSelector(String linkSelector) {
        this.linkSelector = linkSelector;
    }

    public String getImageSelector() {
        return imageSelector;
    }

    public void setImageSelector(String imageSelector) {
        this.imageSelector = imageSelector;
    }

    public String getDescriptionSelector() {
        return descriptionSelector;
    }

    public void setDescriptionSelector(String descriptionSelector) {
        this.descriptionSelector = descriptionSelector;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getDateLocale() {
        return dateLocale;
    }

    public void setDateLocale(String dateLocale) {
        this.dateLocale = dateLocale;
    }

    public String getExtractionMode() {
        return extractionMode;
    }

    public void setExtractionMode(String extractionMode) {
        this.extractionMode = extractionMode;
    }

    public String getEventTextPattern() {
        return eventTextPattern;
    }

    public void setEventTextPattern(String eventTextPattern) {
        this.eventTextPattern = eventTextPattern;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public Boolean getRequiresDetailPageFetch() {
        return requiresDetailPageFetch;
    }

    public void setRequiresDetailPageFetch(Boolean requiresDetailPageFetch) {
        this.requiresDetailPageFetch = requiresDetailPageFetch;
    }

    public String getDetailPageSelector() {
        return detailPageSelector;
    }

    public void setDetailPageSelector(String detailPageSelector) {
        this.detailPageSelector = detailPageSelector;
    }

    public Boolean getAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(Boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public String getAiPrompt() {
        return aiPrompt;
    }

    public void setAiPrompt(String aiPrompt) {
        this.aiPrompt = aiPrompt;
    }

    public Integer getRateLimitMs() {
        return rateLimitMs;
    }

    public void setRateLimitMs(Integer rateLimitMs) {
        this.rateLimitMs = rateLimitMs;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
