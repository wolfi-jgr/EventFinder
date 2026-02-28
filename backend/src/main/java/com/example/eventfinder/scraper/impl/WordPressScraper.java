package com.example.eventfinder.scraper.impl;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.scraper.EventScraper;
import com.example.eventfinder.scraper.ScrapedEvent;
import com.example.eventfinder.scraper.ScraperConfig;
import com.example.eventfinder.scraper.utils.EventMapper;
import com.example.eventfinder.scraper.util.PriceExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scraper implementation for WordPress REST API.
 * Fetches structured event data from WordPress wp-json endpoint.
 */
@Component
public class WordPressScraper implements EventScraper {
    private static final Logger logger = LoggerFactory.getLogger(WordPressScraper.class);
    
    public WordPressScraper(ScraperConfig config) {
        // No specific configuration needed for WordPress scraper (maybe)
    }
    
    @Override
    public String getName() {
        return "WordPressScraper";
    }
    
    public String getScraperName() {
        return "WordPressScraper";
    }
    
    public String getTargetDomain() {
        return "wordpress";
    }
    
    @Override
    public boolean canHandle(ScrapeRule rule) {
        return "WORDPRESS".equals(rule.getExtractionMode()) 
            || isWordPressSite(rule.getBaseUrl());
    }
    
    @Override
    public List<Event> scrapeFromUrl(ScrapeRule rule, String url) throws Exception {
        logger.info("[{}] Scraping {} using WordPress REST API", getName(), rule.getSiteName());
        return scrapeFromWordPressAPI(rule);
    }
    
    @Override
    public List<Event> scrapeFromHtml(ScrapeRule rule, String html) throws Exception {
        // WordPress scraper prefers API, but can parse HTML if provided
        logger.warn("[{}] HTML provided but WordPress scraper prefers REST API. Using API instead.", getName());
        return scrapeFromWordPressAPI(rule);
    }
    
    /**
     * Check if a site is a WordPress site by testing for REST API availability
     */
    public boolean isWordPressSite(String baseUrl) {
        try {
            String wpJsonUrl = baseUrl.replaceAll("/$", "") + "/wp-json/wp/v2/posts?per_page=1";
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wpJsonUrl))
                .timeout(java.time.Duration.ofSeconds(10))
                .header("User-Agent", "EventFinder-Scraper")
                .GET()
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // If we get a 200 and valid JSON with posts array, it's WordPress
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                // WordPress REST API returns an array of posts at the root level
                return root.isArray() || (root.isObject() && root.has("0"));
            }
        } catch (Exception e) {
            logger.debug("[{}] WordPress detection failed for {}: {}", 
                getName(), baseUrl, e.getMessage());
        }
        return false;
    }
    
    /**
     * Scrape events from WordPress REST API
     */
    private List<Event> scrapeFromWordPressAPI(ScrapeRule rule) throws Exception {
        List<Event> events = new ArrayList<>();
        
        String baseUrl = rule.getBaseUrl().replaceAll("/$", "");
        String wpJsonUrl = baseUrl + "/wp-json/wp/v2/posts?per_page=50&orderby=date&order=desc";
        
        logger.info("[{}] Fetching from WordPress API: {}", getName(), wpJsonUrl);
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wpJsonUrl))
            .timeout(java.time.Duration.ofSeconds(15))
            .header("User-Agent", "EventFinder-Scraper")
            .header("Accept", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("WordPress API returned status " + response.statusCode());
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode postsNode = mapper.readTree(response.body());
        
        // WordPress returns an array of posts
        if (postsNode.isArray()) {
            for (JsonNode post : postsNode) {
                try {
                    Event event = parseWordPressPost(post, rule);
                    if (event != null && event.getTitle() != null && event.getStartDateTime() != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    logger.warn("[{}] Failed to parse WordPress post: {}", getName(), e.getMessage());
                }
            }
        }
        
        logger.info("[{}] Successfully extracted {} events from WordPress API", getName(), events.size());
        return events;
    }
    
    /**
     * Parse a WordPress post into an Event
     */
    private Event parseWordPressPost(JsonNode post, ScrapeRule rule) throws Exception {
        ScrapedEvent scraped = new ScrapedEvent();
        
        // Extract title from rendered HTML
        if (post.has("title") && post.get("title").has("rendered")) {
            String title = post.get("title").get("rendered").asText();
            // Remove HTML tags
            title = title.replaceAll("<[^>]*>", "");
            // Decode HTML entities
            title = org.jsoup.parser.Parser.unescapeEntities(title, false);
            scraped.setTitle(title);
        }
        
        // Extract date from post's date field
        if (post.has("date")) {
            String dateStr = post.get("date").asText();
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
                scraped.setStartDateTime(dateTime);
            } catch (Exception e) {
                logger.debug("[{}] Failed to parse WordPress date: {}", getName(), e.getMessage());
            }
        }
        
        // Try to extract event details from content
        if (post.has("content") && post.get("content").has("rendered")) {
            String content = post.get("content").get("rendered").asText();
            Document contentDoc = Jsoup.parse(content);
            
            // Look for venue information in content
            String text = contentDoc.text();
            extractVenueFromText(scraped, text);
            
            // Look for price information in content
            extractPriceFromText(scraped, text);
        }
        
        // Set link as source URL
        if (post.has("link")) {
            scraped.setSourceUrl(post.get("link").asText());
        }
        
        // Set defaults from rule
        scraped.setCategory(rule.getCategory());
        scraped.setOrganizer(rule.getOrganizer());
        
        return EventMapper.convertToEvent(scraped, rule);
    }
    
    /**
     * Try to extract venue from plain text
     */
    private void extractVenueFromText(ScrapedEvent scraped, String text) {
        // Look for common venue indicators
        Pattern venuePattern = Pattern.compile(
            "(?:venue|location|ort|wo|platz|saal|theater|kino)\\s*[:\\-]\\s*([^.\\n,;]+)", 
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = venuePattern.matcher(text);
        if (matcher.find()) {
            String venue = matcher.group(1).trim();
            if (venue.length() >= 3 && venue.length() <= 100) {
                scraped.setVenue(venue);
            }
        }
    }
    
    /**
     * Try to extract price from plain text
     */
    private void extractPriceFromText(ScrapedEvent scraped, String text) {
        // Look for common price indicators in euros
        Pattern pricePattern = Pattern.compile(
            "(\\d+[.,]\\d{2})?\\s*(?:€|eur|euro)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pricePattern.matcher(text);
        if (matcher.find()) {
            String priceStr = matcher.group(1);
            if (priceStr != null) {
                PriceExtractor.extractPrice(scraped, priceStr);
            }
        }
    }
}
