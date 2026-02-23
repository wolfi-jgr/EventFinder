

package com.example.eventfinder.service;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.repository.EventRepository;
import com.example.eventfinder.repository.ScrapeRuleRepository;
import com.example.eventfinder.scraper.HtmlStorage;
import com.example.eventfinder.scraper.impl.GenericScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the scraping process across multiple websites.
 * Integrates HTML storage, rule-based scraping, and event deduplication.
 */
@Service
public class ScrapeOrchestrationService {
    private static final Logger logger = LoggerFactory.getLogger(ScrapeOrchestrationService.class);

    private final ScrapeRuleRepository ruleRepository;
    private final EventRepository eventRepository;
    private final GenericScraper genericScraper;
    private final HtmlStorage htmlStorage;

    public ScrapeOrchestrationService(
            ScrapeRuleRepository ruleRepository,
            EventRepository eventRepository,
            GenericScraper genericScraper,
            HtmlStorage htmlStorage) {
        this.ruleRepository = ruleRepository;
        this.eventRepository = eventRepository;
        this.genericScraper = genericScraper;
        this.htmlStorage = htmlStorage;
    }

    /**
     * Scrape all enabled websites and save events
     */
    @Transactional
    public Map<String, Object> scrapeAll() {
        Map<String, Object> results = new HashMap<>();
        List<ScrapeRule> enabledRules = ruleRepository.findByEnabledTrue();
        
        logger.info("Starting scraping for {} enabled sites", enabledRules.size());
        
        int totalEvents = 0;
        int totalNew = 0;
        Map<String, Integer> siteResults = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        for (ScrapeRule rule : enabledRules) {
            try {
                logger.info("Scraping: {}", rule.getSiteName());
                ScrapeSiteResult result = scrapeSite(rule);
                
                totalEvents += result.eventCount;
                totalNew += result.newEvents;
                siteResults.put(rule.getSiteName(), result.newEvents);
                
                if (result.error != null) {
                    errors.add(rule.getSiteName() + ": " + result.error);
                }
                
            } catch (Exception e) {
                logger.error("Failed to scrape {}: {}", rule.getSiteName(), e.getMessage(), e);
                errors.add(rule.getSiteName() + ": " + e.getMessage());
            }
        }
        
        results.put("totalScraped", totalEvents);
        results.put("totalNew", totalNew);
        results.put("sitesProcessed", enabledRules.size());
        results.put("siteResults", siteResults);
        results.put("errors", errors);
        results.put("timestamp", LocalDateTime.now());
        
        logger.info("Scraping complete: {} total, {} new events", totalEvents, totalNew);
        
        return results;
    }

    /**
     * Scrape a single website using its rule
     */
    @Transactional
    public ScrapeSiteResult scrapeSite(ScrapeRule rule) {
        ScrapeSiteResult result = new ScrapeSiteResult();
        result.siteName = rule.getSiteName();
        
        try {
            List<Event> events;
            
            // Check if this is a JavaScript-heavy site that needs fresh rendering
            boolean isJsHeavySite = rule.getSiteName().toLowerCase().contains("savedate.io");
            
            // Check if we have cached HTML for today (but skip for JS-heavy sites)
            if (!isJsHeavySite && htmlStorage.exists(rule.getSiteName())) {
                logger.info("Using cached HTML for {}", rule.getSiteName());
                String cachedHtml = htmlStorage.loadHtml(rule.getSiteName());
                
                // Parse cached HTML directly
                events = genericScraper.scrapeWithRule(rule, cachedHtml);
                
            } else {
                logger.info("Fetching fresh HTML for {} (JavaScript-intensive: {})", 
                    rule.getSiteName(), isJsHeavySite);
                
                // Fetch and scrape
                events = genericScraper.scrapeWithRule(rule);
                
                // Store HTML for future offline analysis
                // Fetch HTML once more for storage (GenericScraper already fetched it)
                // still todo: Optimize to avoid double fetch
                try {
                    Document doc = Jsoup.connect(rule.getBaseUrl())
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .timeout(10000)
                            .get();
                    htmlStorage.saveHtml(rule.getSiteName(), doc.html());
                } catch (Exception e) {
                    logger.warn("Failed to save HTML for {}: {}", rule.getSiteName(), e.getMessage());
                }
            }
            
            result.eventCount = events.size();
            
            // Deduplicate and save events
            int newEvents = saveEvents(events);
            result.newEvents = newEvents;
            
            logger.info("Scraped {} from {}, {} new events", events.size(), rule.getSiteName(), newEvents);
            
        } catch (Exception e) {
            logger.error("Error scraping {}: {}", rule.getSiteName(), e.getMessage(), e);
            result.error = e.getMessage();
        }
        
        return result;
    }

    /**
     * Save events with deduplication
     */
    private int saveEvents(List<Event> events) {
        int newCount = 0;
        
        for (Event event : events) {
            try {
                // Check if event already exists by deduplication hash
                String hash = event.getDeduplicationHash();
                if (hash != null && eventRepository.existsByDeduplicationHash(hash)) {
                    logger.debug("Skipping duplicate event: {}", event.getTitle());
                    continue;
                }
                
                // Set timestamps
                event.setCreatedAt(LocalDateTime.now());
                event.setUpdatedAt(LocalDateTime.now());
                
                // Save new event
                eventRepository.save(event);
                newCount++;
                
                logger.debug("Saved new event: {}", event.getTitle());
                
            } catch (Exception e) {
                logger.warn("Failed to save event {}: {}", event.getTitle(), e.getMessage());
            }
        }
        
        return newCount;
    }

    /**
     * Scrape a specific site by name
     */
    @Transactional
    public ScrapeSiteResult scrapeBySiteName(String siteName) {
        Optional<ScrapeRule> ruleOpt = ruleRepository.findBySiteName(siteName);
        
        if (ruleOpt.isEmpty()) {
            ScrapeSiteResult result = new ScrapeSiteResult();
            result.siteName = siteName;
            result.error = "No scrape rule found for site: " + siteName;
            return result;
        }
        
        ScrapeRule rule = ruleOpt.get();
        
        if (!rule.getEnabled()) {
            ScrapeSiteResult result = new ScrapeSiteResult();
            result.siteName = siteName;
            result.error = "Scrape rule is disabled";
            return result;
        }
        
        return scrapeSite(rule);
    }

    /**
     * Get all configured sites with their status
     */
    public List<Map<String, Object>> getSiteStatus() {
        List<ScrapeRule> rules = ruleRepository.findAll();
        
        return rules.stream().map(rule -> {
            Map<String, Object> status = new HashMap<>();
            status.put("siteName", rule.getSiteName());
            status.put("enabled", rule.getEnabled());
            status.put("baseUrl", rule.getBaseUrl());
            status.put("extractionMode", rule.getExtractionMode());
            status.put("aiEnabled", rule.getAiEnabled());
            status.put("hasCachedHtml", htmlStorage.exists(rule.getSiteName()));
            
            // Count events from this site
            long eventCount = eventRepository.countByScrapedFrom(rule.getSiteName());
            status.put("eventCount", eventCount);
            
            return status;
        }).collect(Collectors.toList());
    }

    /**
     * Clear cached HTML for all or specific site
     */
    public void clearCache(String siteName) {
        // still todo: Implement cache clearing in HtmlStorage
        logger.info("Cache clearing requested for: {}", siteName != null ? siteName : "all sites");
    }

    /**
     * Result of scraping a single site
     */
    public static class ScrapeSiteResult {
        public String siteName;
        public int eventCount = 0;
        public int newEvents = 0;
        public String error;
    }
}
