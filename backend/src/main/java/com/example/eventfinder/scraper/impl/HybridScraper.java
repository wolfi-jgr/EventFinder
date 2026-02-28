package com.example.eventfinder.scraper.impl;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.scraper.EventScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hybrid scraper that attempts multiple strategies with fallback.
 * Tries WordPress API first, then falls back to CSS/Regex extraction.
 */
@Component
public class HybridScraper implements EventScraper {
    private static final Logger logger = LoggerFactory.getLogger(HybridScraper.class);
    
    private final WordPressScraper wordPressScraper;
    private final CssSelectorScraper cssSelectorScraper;
    private final RegexScraper regexScraper;
    
    public HybridScraper(
        WordPressScraper wordPressScraper,
        CssSelectorScraper cssSelectorScraper,
        RegexScraper regexScraper
    ) {
        this.wordPressScraper = wordPressScraper;
        this.cssSelectorScraper = cssSelectorScraper;
        this.regexScraper = regexScraper;
    }
    
    @Override
    public String getName() {
        return "HybridScraper";
    }
    
    @Override
    public boolean canHandle(ScrapeRule rule) {
        return "HYBRID".equals(rule.getExtractionMode()) 
            || "AUTO".equals(rule.getExtractionMode());
    }
    
    @Override
    public List<Event> scrapeFromUrl(ScrapeRule rule, String url) throws Exception {
        logger.info("[{}] Attempting hybrid scraping for {}", getName(), rule.getSiteName());
        
        // 1. Try WordPress API first
        if (wordPressScraper.isWordPressSite(rule.getBaseUrl())) {
            logger.info("[{}] WordPress detected, trying API first", getName());
            try {
                List<Event> events = wordPressScraper.scrapeFromUrl(rule, url);
                if (!events.isEmpty()) {
                    logger.info("[{}] Successfully scraped {} events from WordPress API", 
                        getName(), events.size());
                    return events;
                }
            } catch (Exception e) {
                logger.warn("[{}] WordPress API scraping failed: {}", getName(), e.getMessage());
            }
        }
        
        // 2. Try regex extraction if pattern is configured
        if (rule.getEventTextPattern() != null && regexScraper.canHandle(rule)) {
            logger.info("[{}] Falling back to regex extraction", getName());
            try {
                List<Event> events = regexScraper.scrapeFromUrl(rule, url);
                if (!events.isEmpty()) {
                    logger.info("[{}] Successfully scraped {} events using regex", 
                        getName(), events.size());
                    return events;
                }
            } catch (Exception e) {
                logger.warn("[{}] Regex scraping failed: {}", getName(), e.getMessage());
            }
        }
        
        // 3. Fall back to CSS selector extraction
        if (rule.getEventItemSelector() != null && cssSelectorScraper.canHandle(rule)) {
            logger.info("[{}] Falling back to CSS selector extraction", getName());
            List<Event> events = cssSelectorScraper.scrapeFromUrl(rule, url);
            logger.info("[{}] CSS selector extraction returned {} events", 
                getName(), events.size());
            return events;
        }
        
        logger.error("[{}] All scraping strategies failed for {}", getName(), rule.getSiteName());
        throw new IllegalStateException("No scraping strategy succeeded for " + rule.getSiteName());
    }
    
    @Override
    public List<Event> scrapeFromHtml(ScrapeRule rule, String html) throws Exception {
        logger.info("[{}] Attempting hybrid scraping from cached HTML for {}", 
            getName(), rule.getSiteName());
        
        // 1. Try WordPress API first (prefer fresh API data even with cache)
        if (wordPressScraper.isWordPressSite(rule.getBaseUrl())) {
            logger.info("[{}] WordPress detected, trying API (ignoring cache)", getName());
            try {
                List<Event> events = wordPressScraper.scrapeFromUrl(rule, rule.getBaseUrl());
                if (!events.isEmpty()) {
                    logger.info("[{}] Successfully scraped {} events from WordPress API", 
                        getName(), events.size());
                    return events;
                }
            } catch (Exception e) {
                logger.warn("[{}] WordPress API scraping failed, using cached HTML: {}", 
                    getName(), e.getMessage());
            }
        }
        
        // 2. Try regex extraction from cached HTML
        if (rule.getEventTextPattern() != null && regexScraper.canHandle(rule)) {
            logger.info("[{}] Trying regex extraction from cached HTML", getName());
            try {
                List<Event> events = regexScraper.scrapeFromHtml(rule, html);
                if (!events.isEmpty()) {
                    logger.info("[{}] Successfully scraped {} events using regex", 
                        getName(), events.size());
                    return events;
                }
            } catch (Exception e) {
                logger.warn("[{}] Regex scraping failed: {}", getName(), e.getMessage());
            }
        }
        
        // 3. Fall back to CSS selector extraction from cached HTML
        if (rule.getEventItemSelector() != null && cssSelectorScraper.canHandle(rule)) {
            logger.info("[{}] Falling back to CSS selector extraction from cached HTML", getName());
            List<Event> events = cssSelectorScraper.scrapeFromHtml(rule, html);
            logger.info("[{}] CSS selector extraction returned {} events", 
                getName(), events.size());
            return events;
        }
        
        logger.error("[{}] All scraping strategies failed for {}", getName(), rule.getSiteName());
        throw new IllegalStateException("No scraping strategy succeeded for " + rule.getSiteName());
    }
}
