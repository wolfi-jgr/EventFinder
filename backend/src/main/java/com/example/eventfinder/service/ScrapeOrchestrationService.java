

package com.example.eventfinder.service;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.repository.EventRepository;
import com.example.eventfinder.repository.ScrapeRuleRepository;
import com.example.eventfinder.scraper.EventScraper;
import com.example.eventfinder.scraper.HtmlStorage;
import com.example.eventfinder.scraper.ScraperFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the scraping process across multiple websites.
 * Integrates HTML storage, rule-based scraping, and event deduplication.
 * Uses ScraperFactory to select the appropriate scraper based on extraction mode.
 */
@Service
public class ScrapeOrchestrationService {
    private static final Logger logger = LoggerFactory.getLogger(ScrapeOrchestrationService.class);

    private final ScrapeRuleRepository ruleRepository;
    private final EventRepository eventRepository;
    private final ScraperFactory scraperFactory;
    private final HtmlStorage htmlStorage;

    public ScrapeOrchestrationService(
            ScrapeRuleRepository ruleRepository,
            EventRepository eventRepository,
            ScraperFactory scraperFactory,
            HtmlStorage htmlStorage) {
        this.ruleRepository = ruleRepository;
        this.eventRepository = eventRepository;
        this.scraperFactory = scraperFactory;
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
            // Select appropriate scraper based on extraction mode
            EventScraper scraper = scraperFactory.getScraper(rule);
            logger.info("Using {} for {}", scraper.getName(), rule.getSiteName());
            
            List<Event> events;
            
            // Check if we have cached HTML for today
            if (htmlStorage.exists(rule.getSiteName())) {
                logger.info("Using cached HTML for {}", rule.getSiteName());
                String cachedHtml = htmlStorage.loadHtml(rule.getSiteName());
                
                // Parse cached HTML using selected scraper
                events = scraper.scrapeFromHtml(rule, cachedHtml);
                
            } else {
                logger.info("Fetching fresh HTML for {}", rule.getSiteName());
                
                // Fetch and scrape using selected scraper
                events = scraper.scrapeFromUrl(rule, rule.getBaseUrl());
                
                // Store HTML for future offline analysis
                // Note: WordPress scraper uses API so we fetch HTML separately for caching
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
        Optional<ScrapeRule> ruleOpt = resolveRuleBySiteName(siteName);
        
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

    @Transactional
    public Map<String, Object> clearEventsBySiteName(String siteName) {
        Map<String, Object> result = new HashMap<>();

        Optional<ScrapeRule> ruleOpt = resolveRuleBySiteName(siteName);
        if (ruleOpt.isEmpty()) {
            result.put("siteName", siteName);
            result.put("deleted", 0);
            result.put("error", "No scrape rule found for site: " + siteName);
            result.put("timestamp", LocalDateTime.now());
            return result;
        }

        ScrapeRule rule = ruleOpt.get();
        long deleted = eventRepository.deleteByScrapedFrom(rule.getSiteName());

        logger.info("Deleted {} events for site {}", deleted, rule.getSiteName());

        result.put("siteName", rule.getSiteName());
        result.put("deleted", deleted);
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    private Optional<ScrapeRule> resolveRuleBySiteName(String siteName) {
        if (siteName == null) {
            return Optional.empty();
        }

        String raw = siteName.trim();
        if (raw.isEmpty()) {
            return Optional.empty();
        }

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(raw);

        String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
        candidates.add(decoded);

        String slashNormalized = decoded.replace("%2F", "/").replace("%40", "@");
        candidates.add(slashNormalized);

        for (String candidate : candidates) {
            Optional<ScrapeRule> exact = ruleRepository.findBySiteName(candidate);
            if (exact.isPresent()) {
                return exact;
            }
        }

        String canonical = canonicalSiteName(decoded);
        return ruleRepository.findAll().stream()
            .filter(rule -> canonicalSiteName(rule.getSiteName()).equals(canonical))
            .findFirst();
    }

    private String canonicalSiteName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace("%2f", "/").replace("%40", "@");
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
