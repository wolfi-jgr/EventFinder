package com.example.eventfinder.scraper;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for web scraping behavior to avoid getting blocked.
 */
@Configuration
@ConfigurationProperties(prefix = "scraper")
public class ScraperConfig {
    
    /**
     * Minimum delay between requests to the same domain (milliseconds)
     */
    private long minDelayBetweenRequests = 2000; // 2 seconds default
    
    /**
     * Maximum delay between requests (for random delay variation)
     */
    private long maxDelayBetweenRequests = 5000; // 5 seconds default
    
    /**
     * Connection timeout (milliseconds)
     */
    private int connectionTimeout = 10000; // 10 seconds
    
    /**
     * Maximum number of retry attempts for failed requests
     */
    private int maxRetries = 3;
    
    /**
     * User agent string to use for requests
     */
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";
    
    /**
     * Whether to follow redirects
     */
    private boolean followRedirects = true;
    
    /**
     * Maximum number of concurrent scrapers running
     */
    private int maxConcurrentScrapers = 3;
    
    /**
     * Whether scraping is enabled globally
     */
    private boolean enabled = true;

    // Getters and Setters
    
    public long getMinDelayBetweenRequests() {
        return minDelayBetweenRequests;
    }

    public void setMinDelayBetweenRequests(long minDelayBetweenRequests) {
        this.minDelayBetweenRequests = minDelayBetweenRequests;
    }

    public long getMaxDelayBetweenRequests() {
        return maxDelayBetweenRequests;
    }

    public void setMaxDelayBetweenRequests(long maxDelayBetweenRequests) {
        this.maxDelayBetweenRequests = maxDelayBetweenRequests;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public int getMaxConcurrentScrapers() {
        return maxConcurrentScrapers;
    }

    public void setMaxConcurrentScrapers(int maxConcurrentScrapers) {
        this.maxConcurrentScrapers = maxConcurrentScrapers;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get a random delay between min and max delay
     */
    public long getRandomDelay() {
        long range = maxDelayBetweenRequests - minDelayBetweenRequests;
        return minDelayBetweenRequests + (long) (Math.random() * range);
    }
}
