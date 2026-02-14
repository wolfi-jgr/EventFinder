package com.example.eventfinder.scraper.impl;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.scraper.BaseWebScraper;
import com.example.eventfinder.scraper.ScraperConfig;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic scraper that attempts to extract events from common HTML structures.
 * This serves as a fallback and example implementation.
 */
@Component
public class GenericEventScraper extends BaseWebScraper {
    
    public GenericEventScraper(ScraperConfig config) {
        super(config);
    }
    
    @Override
    public String getScraperName() {
        return "Generic Event Scraper";
    }
    
    @Override
    public String getTargetDomain() {
        return "generic";
    }
    
    @Override
    public List<Event> scrapeEvents(String url) throws Exception {
        List<Event> events = new ArrayList<>();
        
        try {
            Document doc = fetchDocument(url);
            
            // Remove cookie banners and overlays that might obstruct content
            removeCookieBanners(doc);
            
            // Try common event container selectors
            Elements eventElements = findEventElements(doc);
            
            logger.info("Found {} potential event elements on {}", eventElements.size(), url);
            
            if (eventElements.isEmpty()) {
                logger.warn("No event elements found. Page title: {}", doc.title());
                logger.debug("Page has {} total elements", doc.getAllElements().size());
            }
            
            int validEvents = 0;
            for (Element element : eventElements) {
                try {
                    Event event = extractEvent(element, url);
                    if (event != null && isValidEvent(event)) {
                        setEventMetadata(event);
                        events.add(event);
                        validEvents++;
                        logger.info("✓ Event #{}: {}", validEvents, event.getTitle());
                    } else {
                        logger.debug("✗ Skipped element - validation failed");
                    }
                } catch (Exception e) {
                    logger.debug("✗ Failed to parse element: {}", e.getMessage());
                }
            }
            
            logger.info("Successfully extracted {}/{} events from {}", validEvents, eventElements.size(), url);
            
        } catch (Exception e) {
            logger.error("Error scraping {}: {}", url, e.getMessage(), e);
            throw e;
        }
        
        return events;
    }
    
    /**
     * Remove cookie banners and overlays that obstruct content
     */
    private void removeCookieBanners(Document doc) {
        // Remove common cookie banner selectors
        doc.select("#cookie-banner, .cookie-banner, .cookie-consent, #cookie-consent").remove();
        doc.select("[class*='cookie' i][class*='banner' i]").remove();
        doc.select("[class*='cookie' i][class*='overlay' i]").remove();
        doc.select("[id*='cookie' i], [class*='gdpr' i]").remove();
        logger.debug("Removed cookie banners and overlays");
    }
    
    /**
     * Try to find event elements using common selectors
     */
    private Elements findEventElements(Document doc) {
        // Try various common selectors for event listings
        String[] selectors = {
            "[itemtype*='Event']", // Schema.org Event markup (most semantic)
            "article[class*='event' i]", // Articles with 'event' in class
            "div[class*='event' i]", // Divs with 'event' in class
            "li[class*='event' i]", // List items with 'event' in class
            ".event-item, .event-card, .event-listing, .event-teaser",
            "article", // Any article elements (common for content)
            ".veranstaltung, [class*='veranstaltung' i]", // German for event
            ".card[class*='teaser' i]", // Teaser cards
            "[class*='teaser' i]", // Any teaser elements
            "li.item, li.card", // List items that are items/cards
            "div[class*='item' i]", // Divs with 'item' in class
            "div[class*='card' i]" // Divs with 'card' in class
        };
        
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            // Accept if we find between 1-500 elements (wider range)
            if (!elements.isEmpty() && elements.size() <= 500) {
                logger.info("✓ Using selector '{}' - found {} elements", selector, elements.size());
                return elements;
            } else if (elements.size() > 500) {
                logger.debug("✗ Selector '{}' returned too many elements ({}), skipping", selector, elements.size());
            }
        }
        
        logger.warn("⚠ No suitable event containers found on page");
        return new Elements();
    }
    
    /**
     * Extract event data from an element
     */
    private Event extractEvent(Element element, String sourceUrl) {
        Event event = new Event();
        
        // Extract title (REQUIRED - fail if missing)
        String title = extractTitle(element);
        if (title == null || title.trim().isEmpty()) {
            return null;
        }
        event.setTitle(title.trim());
        
        // Extract description (OPTIONAL)
        String description = extractDescription(element);
        if (description != null && !description.trim().isEmpty()) {
            event.setDescription(description.trim());
        }
        
        // Extract dates (OPTIONAL - use default if missing)
        LocalDateTime startDate = extractDate(element);
        if (startDate != null) {
            event.setStartDateTime(startDate);
        } else {
            // Default: 7 days from now if no date found
            event.setStartDateTime(LocalDateTime.now().plusDays(7));
        }
        
        // Extract location (OPTIONAL)
        String location = extractLocation(element);
        if (location != null && !location.trim().isEmpty()) {
            event.setLocation(location.trim());
        }
        
        // Extract category (OPTIONAL - use default if missing)
        String category = extractCategory(element);
        event.setCategory(category != null && !category.trim().isEmpty() ? category.trim() : "General");
        
        // Extract URL (try to get specific event URL)
        String eventUrl = extractUrl(element);
        if (eventUrl != null && !eventUrl.isEmpty()) {
            event.setSourceUrl(eventUrl);
        } else {
            event.setSourceUrl(sourceUrl); // Fallback to page URL
        }
        
        // Extract image (OPTIONAL)
        String imageUrl = extractImageUrl(element);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            event.setImageUrl(imageUrl);
        }
        
        // Extract price (OPTIONAL)
        String price = extractPrice(element);
        if (price != null && !price.trim().isEmpty()) {
            event.setPrice(price.trim());
        }
        
        return event;
    }
    
    private String extractTitle(Element element) {
        // Try various title selectors (order by specificity)
        String[] selectors = {
            "[itemprop='name']", // Semantic markup
            "h1, h2, h3, h4, h5", // Headings
            ".title, .event-title, .event-name, .headline",
            "a.title, a.event-title", // Title links
            "a[href]", // Any link (often event titles)
            "strong, b", // Bold text
            "span.title, span.name"
        };
        
        for (String selector : selectors) {
            Element titleEl = element.selectFirst(selector);
            if (titleEl != null) {
                String title = safeText(titleEl);
                if (title != null && title.length() >= 3 && title.length() <= 200) {
                    logger.debug("  Title: '{}' (from: {})", title, selector);
                    return title;
                }
            }
        }
        
        // Last resort: use first line of text content
        String text = element.ownText(); // Only direct text, not nested
        if (text != null && text.trim().length() >= 3) {
            String title = text.trim();
            if (title.length() > 150) title = title.substring(0, 150);
            logger.debug("  Title: '{}' (from: element text)", title);
            return title;
        }
        
        logger.debug("  No title found");
        return null;
    }
    
    private String extractDescription(Element element) {
        String[] selectors = {".description", ".event-description", "p", "[itemprop='description']"};
        for (String selector : selectors) {
            Element descEl = element.selectFirst(selector);
            if (descEl != null) {
                String text = safeText(descEl);
                if (text != null && text.length() > 20) {
                    return text;
                }
            }
        }
        return null;
    }
    
    private LocalDateTime extractDate(Element element) {
        String[] selectors = {"time", ".date", ".event-date", "[itemprop='startDate']"};
        for (String selector : selectors) {
            Element dateEl = element.selectFirst(selector);
            if (dateEl != null) {
                String dateStr = safeAttr(dateEl, "datetime");
                if (dateStr == null) {
                    dateStr = safeText(dateEl);
                }
                
                if (dateStr != null) {
                    LocalDateTime parsed = parseDate(dateStr);
                    if (parsed != null) {
                        return parsed;
                    }
                }
            }
        }
        return null;
    }
    
    private LocalDateTime parseDate(String dateStr) {
        // Try various date formats
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (Exception e) {
                // Try next format
            }
        }
        
        return null;
    }
    
    private String extractLocation(Element element) {
        String[] selectors = {".location", ".venue", ".event-location", "[itemprop='location']"};
        for (String selector : selectors) {
            Element locEl = element.selectFirst(selector);
            if (locEl != null) {
                return safeText(locEl);
            }
        }
        return null;
    }
    
    private String extractCategory(Element element) {
        String[] selectors = {".category", ".tag", ".event-category", "[itemprop='category']"};
        for (String selector : selectors) {
            Element catEl = element.selectFirst(selector);
            if (catEl != null) {
                return safeText(catEl);
            }
        }
        return null;
    }
    
    private String extractUrl(Element element) {
        Element link = element.selectFirst("a[href]");
        if (link != null) {
            String href = link.attr("abs:href");
            if (href != null && !href.isEmpty()) {
                return href;
            }
        }
        return null;
    }
    
    private String extractImageUrl(Element element) {
        Element img = element.selectFirst("img");
        if (img != null) {
            String src = img.attr("abs:src");
            if (src != null && !src.isEmpty()) {
                return src;
            }
        }
        return null;
    }
    
    private String extractPrice(Element element) {
        String[] selectors = {".price", ".cost", ".event-price", "[itemprop='price']"};
        for (String selector : selectors) {
            Element priceEl = element.selectFirst(selector);
            if (priceEl != null) {
                return safeText(priceEl);
            }
        }
        return null;
    }
    
    /**
     * Validate event has minimum required data
     * (Very lenient - only require title)
     */
    private boolean isValidEvent(Event event) {
        if (event == null) return false;
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) return false;
        if (event.getTitle().length() < 3) return false; // Too short
        if (event.getTitle().length() > 300) return false; // Probably garbage
        return true;
    }
}
