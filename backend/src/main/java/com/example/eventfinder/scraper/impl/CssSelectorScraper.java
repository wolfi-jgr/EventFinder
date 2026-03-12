package com.example.eventfinder.scraper.impl;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.scraper.EventScraper;
import com.example.eventfinder.scraper.ScrapedEvent;
import com.example.eventfinder.scraper.ScraperConfig;
import com.example.eventfinder.scraper.utils.DateParser;
import com.example.eventfinder.scraper.utils.EventMapper;
import com.example.eventfinder.scraper.util.HtmlExtractor;
import com.example.eventfinder.scraper.util.PriceExtractor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scraper implementation for CSS selector-based extraction.
 * Uses configured selectors to extract structured event data from HTML.
 */
@Component
public class CssSelectorScraper extends HtmlBasedScraper implements EventScraper {
    private static final Logger logger = LoggerFactory.getLogger(CssSelectorScraper.class);
    private static final Pattern HREF_PATTERN = Pattern.compile("href\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_HREF_PATTERN = Pattern.compile("data-href\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    
    public CssSelectorScraper(ScraperConfig config) {
        super(config);
    }
    
    @Override
    public String getName() {
        return "CssSelectorScraper";
    }
    
    @Override
    public boolean canHandle(ScrapeRule rule) {
        return "CSS_SELECTOR".equals(rule.getExtractionMode()) 
            && rule.getEventItemSelector() != null;
    }
    
    @Override
    public List<Event> scrapeFromUrl(ScrapeRule rule, String url) throws Exception {
        logger.info("[{}] Scraping {} using CSS selectors", getName(), rule.getSiteName());
        Document doc = fetchHtmlDocument(url);
        return scrapeFromHtml(rule, doc.html());
    }
    
    @Override
    public List<Event> scrapeFromHtml(ScrapeRule rule, String html) throws Exception {
        Document doc = parseHtmlDocument(html, rule.getBaseUrl());
        return extractWithSelectors(doc, rule, html);
    }
    
    /**
     * Extract events using CSS selectors from rule
     */
    private List<Event> extractWithSelectors(Document doc, ScrapeRule rule, String rawHtml) {
        List<Event> events = new ArrayList<>();
        
        // Find event items using configured selector
        Elements eventItems = doc.select(rule.getEventItemSelector());
        logger.debug("[{}] Found {} event items for {}", getName(), eventItems.size(), rule.getSiteName());
        
        for (Element item : eventItems) {
            try {
                ScrapedEvent scraped = new ScrapedEvent();

                String linkHref = extractLinkHref(item, rule);
                
                // Extract title
                if (rule.getTitleSelector() != null) {
                    Element titleEl = item.selectFirst(rule.getTitleSelector());
                    scraped.setTitle(HtmlExtractor.safeText(titleEl));
                }
                
                // Extract date/time
                if (rule.getDateSelector() != null) {
                    Elements dateEls = item.select(rule.getDateSelector());
                    String dateStr;
                    if (!dateEls.isEmpty()) {
                        dateStr = dateEls.eachText().stream()
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.joining(" "));
                    } else {
                        Element dateEl = item.selectFirst(rule.getDateSelector());
                        dateStr = HtmlExtractor.safeText(dateEl);
                    }

                    LocalDateTime dateTime = DateParser.parseDateTime(dateStr, rule.getDateFormat(), rule.getDateLocale());
                    if (dateTime == null && linkHref != null) {
                        dateTime = DateParser.parseDateFromUrl(linkHref);
                    }
                    scraped.setStartDateTime(dateTime);
                }
                
                // Extract price
                if (rule.getPriceSelector() != null) {
                    Element priceEl = item.selectFirst(rule.getPriceSelector());
                    String priceStr = HtmlExtractor.safeText(priceEl);
                    PriceExtractor.extractPrice(scraped, priceStr);
                }
                
                // Extract venue
                if (rule.getVenueSelector() != null) {
                    Element venueEl = item.selectFirst(rule.getVenueSelector());
                    String venue = HtmlExtractor.safeText(venueEl);
                    // Validate and clean venue name
                    if (venue != null && HtmlExtractor.isValidVenueName(venue)) {
                        scraped.setVenue(venue.trim());
                    }
                }
                
                // Extract link
                if (linkHref != null) {
                    scraped.setSourceUrl(linkHref);
                }
                
                // Extract image
                if (rule.getImageSelector() != null) {
                    Element imgEl = item.selectFirst(rule.getImageSelector());
                    scraped.setImageUrl(HtmlExtractor.safeAttr(imgEl, "src"));
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
                logger.warn("[{}] Failed to parse event item: {}", getName(), e.getMessage());
            }
        }
        
        logger.info("[{}] Successfully extracted {} events from {}", getName(), events.size(), rule.getSiteName());
        return events;
    }

    private String extractLinkHref(Element item, ScrapeRule rule) {
        if (rule.getLinkSelector() == null || rule.getLinkSelector().isBlank()) {
            return null;
        }

        String selector = rule.getLinkSelector().trim();
        Element linkElement;

        if ("this".equalsIgnoreCase(selector)) {
            linkElement = item;
        } else {
            linkElement = item.selectFirst(selector);
        }

        String linkHref = HtmlExtractor.safeAttr(linkElement, "href");
        if (linkHref == null) {
            linkHref = HtmlExtractor.safeAttr(linkElement, "data-href");
        }
        if (linkHref == null && linkElement != null) {
            linkHref = extractHrefFromHtml(linkElement.outerHtml());
        }
        if (linkHref == null) {
            linkHref = extractHrefFromHtml(item.outerHtml());
        }

        return resolveAbsoluteUrl(linkHref, rule.getBaseUrl());
    }

    private String extractHrefFromHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }

        Matcher hrefMatcher = HREF_PATTERN.matcher(html);
        if (hrefMatcher.find()) {
            return hrefMatcher.group(1);
        }

        Matcher dataHrefMatcher = DATA_HREF_PATTERN.matcher(html);
        if (dataHrefMatcher.find()) {
            return dataHrefMatcher.group(1);
        }

        return null;
    }

    private String resolveAbsoluteUrl(String linkHref, String baseUrl) {
        if (linkHref == null || linkHref.isBlank()) {
            return null;
        }

        if (linkHref.startsWith("http://") || linkHref.startsWith("https://")) {
            return linkHref;
        }

        try {
            return new java.net.URL(new java.net.URL(baseUrl), linkHref).toString();
        } catch (Exception ex) {
            logger.debug("[{}] Could not resolve relative link '{}' against base '{}': {}", getName(), linkHref, baseUrl, ex.getMessage());
            return linkHref;
        }
    }
}
