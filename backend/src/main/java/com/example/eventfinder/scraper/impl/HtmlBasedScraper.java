package com.example.eventfinder.scraper.impl;

import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.scraper.BaseWebScraper;
import com.example.eventfinder.scraper.ScraperConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for scrapers that work with HTML content.
 * Provides common document fetching and parsing logic using composition.
 */
public abstract class HtmlBasedScraper {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final BaseWebScraper webScraper;
    protected final ScraperConfig config;
    
    public HtmlBasedScraper(ScraperConfig config) {
        this.config = config;
        this.webScraper = new DocumentFetcher(config);
    }
    
    /**
     * Fetch HTML document from URL with rate limiting.
     */
    protected Document fetchHtmlDocument(String url) throws Exception {
        return webScraper.fetchDocument(url);
    }
    
    /**
     * Parse HTML string into Document.
     */
    protected Document parseHtmlDocument(String html, String baseUrl) {
        return Jsoup.parse(html, baseUrl);
    }
    
    /**
     * Validate that the rule has required CSS selector configuration.
     */
    protected void validateCssSelectorRule(ScrapeRule rule) throws IllegalArgumentException {
        if (rule.getEventItemSelector() == null || rule.getEventItemSelector().isEmpty()) {
            throw new IllegalArgumentException(
                "CSS Selector scraper requires 'eventItemSelector' in rule for " + rule.getSiteName()
            );
        }
    }
    
    /**
     * Validate that the rule has required regex configuration.
     */
    protected void validateRegexRule(ScrapeRule rule) throws IllegalArgumentException {
        if (rule.getEventTextPattern() == null || rule.getEventTextPattern().isEmpty()) {
            throw new IllegalArgumentException(
                "Regex scraper requires 'eventTextPattern' in rule for " + rule.getSiteName()
            );
        }
    }
    
    /**
     * Inner class that provides document fetching functionality.
     * This is a minimal implementation of BaseWebScraper for the new architecture.
     */
    private static class DocumentFetcher extends BaseWebScraper {
        
        public DocumentFetcher(ScraperConfig config) {
            super(config);
        }
        
        @Override
        public String getScraperName() {
            return "DocumentFetcher";
        }
        
        @Override
        public String getTargetDomain() {
            return "generic";
        }
        
        @Override
        public java.util.List<com.example.eventfinder.model.Event> scrapeEvents(String url) throws Exception {
            throw new UnsupportedOperationException("DocumentFetcher is only for fetching documents");
        }
    }
}
