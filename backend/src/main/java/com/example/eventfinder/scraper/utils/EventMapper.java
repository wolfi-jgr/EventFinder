package com.example.eventfinder.scraper.utils;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.scraper.ScrapedEvent;

import java.time.LocalDateTime;

/**
 * Utility class for converting ScrapedEvent to Event entity.
 * Handles metadata, deduplication hashing, and field mapping.
 */
public class EventMapper {
    private static final String PARSER_VERSION = "ScraperV2";
    
    /**
     * Convert ScrapedEvent to Event entity with rule metadata
     */
    public static Event convertToEvent(ScrapedEvent scraped, ScrapeRule rule) {
        Event event = new Event();
        
        // Basic fields
        event.setTitle(scraped.getTitle());
        event.setDescription(scraped.getDescription());
        event.setStartDateTime(scraped.getStartDateTime());
        event.setEndDateTime(scraped.getEndDateTime());
        event.setLocation(scraped.getLocation());
        event.setLatitude(scraped.getLatitude());
        event.setLongitude(scraped.getLongitude());
        event.setCategory(scraped.getCategory());
        event.setImageUrl(scraped.getImageUrl());
        event.setOrganizer(scraped.getOrganizer());
        event.setSourceUrl(scraped.getSourceUrl());
        
        // Pricing
        event.setPriceFrom(scraped.getPriceFrom());
        event.setPriceTo(scraped.getPriceTo());
        event.setPriceNote(scraped.getPriceNote());
        
        // Venue/room info
        event.setVenue(scraped.getVenue());
        
        // Recurrence
        event.setIsRecurring(scraped.getIsRecurring() != null ? scraped.getIsRecurring() : false);
        event.setRecurringPattern(scraped.getRecurringPattern());
        
        // Tags
        event.setTags(scraped.getTags());
        
        // Raw HTML for offline re-parsing (truncate if too long)
        String rawHtml = scraped.getRawHtml();
        if (rawHtml != null && rawHtml.length() > 50000) {
            rawHtml = rawHtml.substring(0, 50000);
        }
        event.setRawSourceHtml(rawHtml);
        
        // Scraper metadata
        event.setScrapedFrom(rule.getSiteName());
        event.setParserVersion(PARSER_VERSION);
        event.setUpdatedAt(LocalDateTime.now());
        
        // Deduplication hash
        event.setDeduplicationHash(
            calculateDeduplicationHash(event.getTitle(), event.getStartDateTime(), event.getOrganizer())
        );
        
        return event;
    }
    
    /**
     * Calculate deduplication hash for event identification
     */
    public static String calculateDeduplicationHash(String title, LocalDateTime startDateTime, String organizer) {
        if (title == null || startDateTime == null) {
            return null;
        }
        
        String normalized = (normalizeText(title) + "_" + startDateTime.toString() + "_" + normalizeText(organizer))
            .toLowerCase();
        
        return Integer.toHexString(normalized.hashCode());
    }
    
    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
}
