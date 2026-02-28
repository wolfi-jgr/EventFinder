package com.example.eventfinder.scraper.impl;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.scraper.EventScraper;
import com.example.eventfinder.scraper.ScrapedEvent;
import com.example.eventfinder.scraper.ScraperConfig;
import com.example.eventfinder.scraper.utils.DateParser;
import com.example.eventfinder.scraper.utils.EventMapper;
import com.example.eventfinder.scraper.util.PriceExtractor;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scraper implementation for regex pattern-based extraction.
 * Extracts events by matching text patterns against page content.
 */
@Component
public class RegexScraper extends HtmlBasedScraper implements EventScraper {
    private static final Logger logger = LoggerFactory.getLogger(RegexScraper.class);
    
    public RegexScraper(ScraperConfig config) {
        super(config);
    }
    
    @Override
    public String getName() {
        return "RegexScraper";
    }
    
    @Override
    public boolean canHandle(ScrapeRule rule) {
        return "REGEX".equals(rule.getExtractionMode()) 
            && rule.getEventTextPattern() != null;
    }
    
    @Override
    public List<Event> scrapeFromUrl(ScrapeRule rule, String url) throws Exception {
        logger.info("[{}] Scraping {} using regex patterns", getName(), rule.getSiteName());
        Document doc = fetchHtmlDocument(url);
        return scrapeFromHtml(rule, doc.html());
    }
    
    @Override
    public List<Event> scrapeFromHtml(ScrapeRule rule, String html) throws Exception {
        Document doc = parseHtmlDocument(html, rule.getBaseUrl());
        return extractWithRegex(doc, rule, html);
    }
    
    /**
     * Extract events using regex pattern from rule
     */
    private List<Event> extractWithRegex(Document doc, ScrapeRule rule, String rawHtml) {
        List<Event> events = new ArrayList<>();
        
        String text = doc.text();
        Pattern pattern = Pattern.compile(rule.getEventTextPattern());
        Matcher matcher = pattern.matcher(text);
        
        logger.debug("[{}] Searching for pattern in {} characters of text", getName(), text.length());
        
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
            try {
                ScrapedEvent scraped = new ScrapedEvent();
                
                // Extract groups from regex
                if (matcher.groupCount() >= 1) {
                    scraped.setTitle(matcher.group(1));
                }
                if (matcher.groupCount() >= 2) {
                    String dateStr = matcher.group(2);
                    scraped.setStartDateTime(
                        DateParser.parseDateTime(dateStr, rule.getDateFormat(), rule.getDateLocale())
                    );
                }
                if (matcher.groupCount() >= 3) {
                    PriceExtractor.extractPrice(scraped, matcher.group(3));
                }
                if (matcher.groupCount() >= 4) {
                    scraped.setVenue(matcher.group(4));
                }
                
                // Set defaults from rule
                scraped.setCategory(rule.getCategory());
                scraped.setOrganizer(rule.getOrganizer());
                scraped.setRawHtml(rawHtml);
                
                // Only add if we have at least title and date
                if (scraped.getTitle() != null && scraped.getStartDateTime() != null) {
                    Event event = EventMapper.convertToEvent(scraped, rule);
                    events.add(event);
                }
                
            } catch (Exception e) {
                logger.warn("[{}] Failed to parse regex match: {}", getName(), e.getMessage());
            }
        }
        
        logger.info("[{}] Found {} matches, extracted {} valid events from {}", 
            getName(), matchCount, events.size(), rule.getSiteName());
        return events;
    }

}

