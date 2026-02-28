package com.example.eventfinder.scraper;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.ScrapeRule;

import java.util.List;

/**
 * Strategy interface for different scraping approaches.
 * Each implementation handles a specific extraction mode or site type.
 */
public interface EventScraper {
    
    /**
     * Scrape events from the given URL.
     *
     * @param rule The scrape rule configuration
     * @param url  The URL to scrape
     * @return List of extracted events
     * @throws Exception If scraping fails
     */
    List<Event> scrapeFromUrl(ScrapeRule rule, String url) throws Exception;
    
    /**
     * Scrape events from cached/pre-fetched HTML.
     *
     * @param rule The scrape rule configuration
     * @param html The HTML content to parse
     * @return List of extracted events
     * @throws Exception If parsing fails
     */
    List<Event> scrapeFromHtml(ScrapeRule rule, String html) throws Exception;
    
    /**
     * Get the name of this scraper for logging.
     */
    String getName();
    
    /**
     * Check if this scraper can handle the given rule.
     * Factory uses this to select the right scraper.
     */
    boolean canHandle(ScrapeRule rule);
}
