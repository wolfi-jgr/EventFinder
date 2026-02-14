package com.example.eventfinder.scraper;

import com.example.eventfinder.model.Event;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for web scrapers with built-in rate limiting and blocking prevention.
 */
public abstract class BaseWebScraper {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ScraperConfig config;
    
    // Track last request time per domain to enforce rate limiting
    private static final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    
    // List of common user agents to rotate through
    private static final List<String> USER_AGENTS = Arrays.asList(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
    );
    
    public BaseWebScraper(ScraperConfig config) {
        this.config = config;
    }
    
    /**
     * Get the name of this scraper (for logging)
     */
    public abstract String getScraperName();
    
    /**
     * Get the base domain this scraper targets (e.g., "eventbrite.com")
     */
    public abstract String getTargetDomain();
    
    /**
     * Scrape events from the given URL
     */
    public abstract List<Event> scrapeEvents(String url) throws Exception;
    
    /**
     * Fetch a webpage with rate limiting and retry logic
     */
    protected Document fetchDocument(String url) throws IOException, InterruptedException {
        if (!config.isEnabled()) {
            throw new IllegalStateException("Scraping is disabled");
        }
        
        String domain = extractDomain(url);
        enforceRateLimit(domain);
        
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < config.getMaxRetries()) {
            try {
                logger.info("[{}] Fetching: {} (attempt {}/{})", 
                    getScraperName(), url, attempt + 1, config.getMaxRetries());
                
                Connection connection = Jsoup.connect(url)
                    .userAgent(getRandomUserAgent())
                    .timeout(config.getConnectionTimeout())
                    .followRedirects(config.isFollowRedirects())
                    .ignoreHttpErrors(false)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1");
                
                Document doc = connection.get();
                
                logger.info("[{}] Successfully fetched: {}", getScraperName(), url);
                return doc;
                
            } catch (IOException e) {
                lastException = e;
                attempt++;
                
                if (attempt < config.getMaxRetries()) {
                    long backoffDelay = (long) (config.getMinDelayBetweenRequests() * Math.pow(2, attempt));
                    logger.warn("[{}] Request failed (attempt {}/{}): {}. Retrying in {}ms...", 
                        getScraperName(), attempt, config.getMaxRetries(), e.getMessage(), backoffDelay);
                    Thread.sleep(backoffDelay);
                } else {
                    logger.error("[{}] All retry attempts exhausted for: {}", getScraperName(), url);
                }
            }
        }
        
        throw new IOException("Failed to fetch " + url + " after " + config.getMaxRetries() + " attempts", lastException);
    }
    
    /**
     * Enforce rate limiting by domain
     */
    private void enforceRateLimit(String domain) throws InterruptedException {
        Long lastRequest = lastRequestTime.get(domain);
        
        if (lastRequest != null) {
            long timeSinceLastRequest = System.currentTimeMillis() - lastRequest;
            long requiredDelay = config.getRandomDelay();
            
            if (timeSinceLastRequest < requiredDelay) {
                long waitTime = requiredDelay - timeSinceLastRequest;
                logger.debug("[{}] Rate limiting: waiting {}ms before next request to {}", 
                    getScraperName(), waitTime, domain);
                Thread.sleep(waitTime);
            }
        }
        
        lastRequestTime.put(domain, System.currentTimeMillis());
    }
    
    /**
     * Extract domain from URL
     */
    private String extractDomain(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (Exception e) {
            return urlString;
        }
    }
    
    /**
     * Get a random user agent from the pool
     */
    private String getRandomUserAgent() {
        return USER_AGENTS.get(new Random().nextInt(USER_AGENTS.size()));
    }
    
    /**
     * Helper method to safely parse text from elements
     */
    protected String safeText(org.jsoup.nodes.Element element) {
        return element != null ? element.text().trim() : null;
    }
    
    /**
     * Helper method to safely get attributes
     */
    protected String safeAttr(org.jsoup.nodes.Element element, String attribute) {
        return element != null ? element.attr(attribute).trim() : null;
    }
    
    /**
     * Set last updated timestamp on event
     */
    protected void setEventMetadata(Event event) {
        event.setLastUpdated(Instant.now());
        if (event.getStatus() == null) {
            event.setStatus("SCHEDULED");
        }
    }
}
