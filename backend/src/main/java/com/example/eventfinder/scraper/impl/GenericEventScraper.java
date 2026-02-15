package com.example.eventfinder.scraper.impl;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.scraper.BaseWebScraper;
import com.example.eventfinder.scraper.ScraperConfig;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            "div[class*='event-item' i]", // Divs with 'event-item' in class
            "div[class*='event-card' i]", // Divs with 'event-card' in class
            "li[class*='event' i]", // List items with 'event' in class
            ".event-item, .event-card, .event-listing, .event-teaser",
            "[class*='veranstaltung' i]", // German for event
            "article[class*='teaser' i]", // Article teasers
            "a.teaser", // Teaser links
            "div.teaser", // Teaser divs
            "article.teaser", // Teaser articles
            "li.teaser", // Teaser list items
            "article[class*='card' i]", // Article cards
            "div[class*='card' i]:has(h2, h3, h4, a)", // Cards with titles
            "article", // Any article elements (common for content)
            "li[class*='item' i]", // List items
            "div[class*='list-item' i]" // List item divs
        };
        
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            // Accept if we find between 1-500 elements (wider range)
            if (!elements.isEmpty() && elements.size() <= 500) {
                // Filter out obviously wrong elements (like navigation, footers, etc.)
                Elements filtered = new Elements();
                for (Element el : elements) {
                    String className = el.className().toLowerCase();
                    String id = el.id().toLowerCase();
                    // Skip navigation, footer, header, sidebar elements
                    if (!className.contains("nav") && !className.contains("menu") && 
                        !className.contains("footer") && !className.contains("header") &&
                        !className.contains("sidebar") && !id.contains("nav") &&
                        !id.contains("menu") && !id.contains("footer")) {
                        filtered.add(el);
                    }
                }
                
                if (!filtered.isEmpty() && filtered.size() <= 500) {
                    logger.info("✓ Using selector '{}' - found {} elements", selector, filtered.size());
                    return filtered;
                }
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
            logger.debug("  Using extracted date: {}", startDate);
        } else {
            // Default: today at 7 PM if no date found
            // This is more reasonable than a future date since we're scraping current events
            LocalDateTime defaultDate = LocalDate.now().atTime(19, 0);
            event.setStartDateTime(defaultDate);
            logger.debug("  No date found, using default: {}", defaultDate);
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
        String[] selectors = {"time", ".date", ".event-date", ".event-time", "[itemprop='startDate']", ".datetime"};
        
        for (String selector : selectors) {
            Element dateEl = element.selectFirst(selector);
            if (dateEl != null) {
                // Try datetime attribute first (most reliable)
                String dateStr = safeAttr(dateEl, "datetime");
                if (dateStr == null) {
                    dateStr = safeText(dateEl);
                }
                
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    LocalDateTime parsed = parseDate(dateStr);
                    if (parsed != null) {
                        logger.debug("  Date: {} (from: {}, text: '{}')", parsed, selector, dateStr);
                        return parsed;
                    }
                }
            }
        }
        
        // Also try finding date in parent element or siblings
        String allText = element.text();
        LocalDateTime parsed = parseDate(allText);
        if (parsed != null) {
            logger.debug("  Date: {} (from: element text)", parsed);
            return parsed;
        }
        
        return null;
    }
    
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        dateStr = dateStr.trim();
        logger.debug("  Attempting to parse date: '{}'", dateStr);
        
        // 1. Try ISO formats first (most reliable)
        LocalDateTime iso = tryISOFormats(dateStr);
        if (iso != null) return iso;
        
        // 2. Try German relative dates (Heute, Morgen, etc.)
        LocalDateTime relative = tryRelativeGermanDates(dateStr);
        if (relative != null) return relative;
        
        // 3. Try German day names (Montag, Dienstag, etc.)
        LocalDateTime dayName = tryGermanDayNames(dateStr);
        if (dayName != null) return dayName;
        
        // 4. Try various German date formats
        LocalDateTime german = tryGermanDateFormats(dateStr);
        if (german != null) return german;
        
        // 5. Try extracting date patterns with regex
        LocalDateTime regex = tryRegexDateExtraction(dateStr);
        if (regex != null) return regex;
        
        logger.debug("  Could not parse date from: '{}'", dateStr);
        return null;
    }
    
    private LocalDateTime tryISOFormats(String dateStr) {
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                if (dateStr.length() == 10) { // Just a date
                    return LocalDate.parse(dateStr, formatter).atStartOfDay();
                }
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        return null;
    }
    
    private LocalDateTime tryRelativeGermanDates(String dateStr) {
        String lower = dateStr.toLowerCase();
        LocalDate today = LocalDate.now();
        LocalTime defaultTime = LocalTime.of(19, 0); // Default 7 PM
        
        // Extract time if present
        LocalTime time = extractTime(dateStr);
        if (time == null) time = defaultTime;
        
        if (lower.contains("heute")) {
            return LocalDateTime.of(today, time);
        }
        if (lower.contains("morgen")) {
            return LocalDateTime.of(today.plusDays(1), time);
        }
        if (lower.contains("übermorgen")) {
            return LocalDateTime.of(today.plusDays(2), time);
        }
        
        return null;
    }
    
    private LocalDateTime tryGermanDayNames(String dateStr) {
        String lower = dateStr.toLowerCase().replaceAll("[.,]", "");
        LocalDate today = LocalDate.now();
        LocalTime time = extractTime(dateStr);
        if (time == null) time = LocalTime.of(19, 0);
        
        Map<String, DayOfWeek> dayMap = new HashMap<>();
        dayMap.put("montag", DayOfWeek.MONDAY);
        dayMap.put("mo", DayOfWeek.MONDAY);
        dayMap.put("dienstag", DayOfWeek.TUESDAY);
        dayMap.put("di", DayOfWeek.TUESDAY);
        dayMap.put("mittwoch", DayOfWeek.WEDNESDAY);
        dayMap.put("mi", DayOfWeek.WEDNESDAY);
        dayMap.put("donnerstag", DayOfWeek.THURSDAY);
        dayMap.put("do", DayOfWeek.THURSDAY);
        dayMap.put("freitag", DayOfWeek.FRIDAY);
        dayMap.put("fr", DayOfWeek.FRIDAY);
        dayMap.put("samstag", DayOfWeek.SATURDAY);
        dayMap.put("sa", DayOfWeek.SATURDAY);
        dayMap.put("sonntag", DayOfWeek.SUNDAY);
        dayMap.put("so", DayOfWeek.SUNDAY);
        
        // Find the earliest matching day (in case multiple day names are in the text)
        LocalDate earliestDate = null;
        
        for (Map.Entry<String, DayOfWeek> entry : dayMap.entrySet()) {
            if (lower.contains(entry.getKey())) {
                DayOfWeek targetDay = entry.getValue();
                LocalDate date = today;
                
                // Find next occurrence of this day (including today if it matches)
                while (date.getDayOfWeek() != targetDay) {
                    date = date.plusDays(1);
                    if (date.isAfter(today.plusMonths(1))) break; // Safety limit
                }
                
                if (!date.isAfter(today.plusMonths(1))) {
                    if (earliestDate == null || date.isBefore(earliestDate)) {
                        earliestDate = date;
                    }
                }
            }
        }
        
        if (earliestDate != null) {
            return LocalDateTime.of(earliestDate, time);
        }
        
        return null;
    }
    
    private LocalDateTime tryGermanDateFormats(String dateStr) {
        // Replace German month names with numbers
        String normalized = normalizeGermanMonths(dateStr);
        
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM. HH:mm"),
            DateTimeFormatter.ofPattern("d.M.yyyy HH:mm"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("dd. MMMM yyyy HH:mm", Locale.GERMAN),
            DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN),
            DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN),
            DateTimeFormatter.ofPattern("dd. MMM yyyy", Locale.GERMAN),
            DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN),
            DateTimeFormatter.ofPattern("d.M.", Locale.GERMAN)
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                // Check if format includes time
                if (formatter.toString().contains("HH")) {
                    return LocalDateTime.parse(normalized, formatter);
                } else {
                    LocalDate date = LocalDate.parse(normalized, formatter);
                    // If year is missing, assume current or next year
                    if (normalized.matches("\\d{1,2}\\.\\d{1,2}\\.?")) {
                        if (date.isBefore(LocalDate.now().minusMonths(1))) {
                            date = date.plusYears(1);
                        }
                    }
                    LocalTime time = extractTime(dateStr);
                    return LocalDateTime.of(date, time != null ? time : LocalTime.of(19, 0));
                }
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        return null;
    }
    
    private LocalDateTime tryRegexDateExtraction(String dateStr) {
        // Try to extract date patterns like "17.02.2026" or "17.02."
        Pattern datePattern = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.?(\\d{4})?");
        Matcher matcher = datePattern.matcher(dateStr);
        
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = matcher.group(3) != null ? 
                    Integer.parseInt(matcher.group(3)) : LocalDate.now().getYear();
                
                LocalDate date = LocalDate.of(year, month, day);
                
                // If date is in the past and no year was specified, try next year
                if (matcher.group(3) == null && date.isBefore(LocalDate.now().minusMonths(1))) {
                    date = date.plusYears(1);
                }
                
                LocalTime time = extractTime(dateStr);
                return LocalDateTime.of(date, time != null ? time : LocalTime.of(19, 0));
            } catch (Exception e) {
                logger.debug("  Regex date extraction failed: {}", e.getMessage());
            }
        }
        
        return null;
    }
    
    private LocalTime extractTime(String text) {
        // Try to find time patterns like "20:00", "20.00", "20:30 Uhr"
        Pattern timePattern = Pattern.compile("(\\d{1,2})[:.](\\d{2})(?:\\s*Uhr)?");
        Matcher matcher = timePattern.matcher(text);
        
        if (matcher.find()) {
            try {
                int hour = Integer.parseInt(matcher.group(1));
                int minute = Integer.parseInt(matcher.group(2));
                
                if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60) {
                    return LocalTime.of(hour, minute);
                }
            } catch (Exception e) {
                // Invalid time
            }
        }
        
        return null;
    }
    
    private String normalizeGermanMonths(String dateStr) {
        Map<String, String> monthMap = new HashMap<>();
        monthMap.put("januar", "01");
        monthMap.put("jänner", "01");
        monthMap.put("februar", "02");
        monthMap.put("märz", "03");
        monthMap.put("april", "04");
        monthMap.put("mai", "05");
        monthMap.put("juni", "06");
        monthMap.put("juli", "07");
        monthMap.put("august", "08");
        monthMap.put("september", "09");
        monthMap.put("oktober", "10");
        monthMap.put("november", "11");
        monthMap.put("dezember", "12");
        
        String lower = dateStr.toLowerCase();
        for (Map.Entry<String, String> entry : monthMap.entrySet()) {
            if (lower.contains(entry.getKey())) {
                // Replace month name but preserve the rest of the string
                return dateStr.replaceAll("(?i)" + entry.getKey(), entry.getValue());
            }
        }
        
        return dateStr;
    }
    
    private String extractLocation(Element element) {
        // Blacklist of invalid location values
        Set<String> blacklist = new HashSet<>(Arrays.asList(
            "location", "ort", "venue", "veranstaltungsort", "address", "wien", "vienna",
            "theater", "kino", "museum", "abenteuer", "wahnsinn", "kostenlos",
            "programm", "eventprogramm", "informationen", "details", "tickets"
        ));
        
        // Try specific location selectors with multiple passes
        String[] selectors = {
            "[itemprop='location'] [itemprop='name']", // Schema.org location name
            "[itemtype*='Place'] [itemprop='name']", // Schema.org place name
            "[itemprop='location']",
            "[itemtype*='Place']",
            ".venue .name", ".location .name", // Venue name within location
            ".venue", ".location", ".event-venue", ".event-location",
            ".place", ".ort", ".veranstaltungsort",
            "address", ".address",
            "svg title", // SVG elements often contain venue names
            ".lining-nums", // Specific to some event sites
            "[class*='location' i] svg title",
            "[class*='venue' i] svg title"
        };
        
        for (String selector : selectors) {
            Elements locEls = element.select(selector);
            for (Element locEl : locEls) {
                String location = safeText(locEl);
                if (isValidVenueName(location, blacklist)) {
                    logger.debug("  Location: '{}' (from: {})", location.trim(), selector);
                    return location.trim();
                }
            }
        }
        
        // Try to find explicit "@" pattern (very specific)
        String text = element.text();
        if (text != null) {
            int atIdx = text.indexOf(" @ ");
            if (atIdx > 0 && atIdx < text.length() - 4) {
                String afterAt = text.substring(atIdx + 3).trim();
                // Take only until first comma, newline, or pipe
                int endIdx = Integer.MAX_VALUE;
                for (char delimiter : new char[]{',', '\n', '\r', '|', ';'}) {
                    int idx = afterAt.indexOf(delimiter);
                    if (idx > 0 && idx < endIdx) {
                        endIdx = idx;
                    }
                }
                
                String location = endIdx < afterAt.length() ? afterAt.substring(0, endIdx).trim() : afterAt.trim();
                if (isValidVenueName(location, blacklist)) {
                    logger.debug("  Location: '{}' (from @ pattern)", location);
                    return location;
                }
            }
            
            // Try pattern matching for venue indicators
            // Look for patterns like "im Theater", "in der Arena", etc.
            Pattern venuePattern = Pattern.compile("(?:im|in der|in dem|beim|am|an der)\\s+([A-ZÄÖÜ][\\w\\s-]{2,60})(?:[,\\.\\|]|$)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = venuePattern.matcher(text);
            if (matcher.find()) {
                String venue = matcher.group(1).trim();
                if (isValidVenueName(venue, blacklist)) {
                    logger.debug("  Location: '{}' (from pattern match)", venue);
                    return venue;
                }
            }
        }
        
        // Last resort: look for capitalized words that might be venue names
        // This catches things like "Burgtheater", "Staatsoper", etc.
        String[] words = text != null ? text.split("\\s+") : new String[0];
        for (String word : words) {
            // Must start with capital, be 5-40 chars, and look like a proper noun
            if (word.length() >= 5 && word.length() <= 40 && 
                Character.isUpperCase(word.charAt(0)) &&
                !word.matches("^(Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember|Montag|Dienstag|Mittwoch|Donnerstag|Freitag|Samstag|Sonntag|Heute|Morgen)$")) {
                // Check if it looks like a venue (contains "theater", "halle", "club", etc.)
                String lowerWord = word.toLowerCase();
                if (lowerWord.contains("theater") || lowerWord.contains("halle") || 
                    lowerWord.contains("arena") || lowerWord.contains("club") ||
                    lowerWord.contains("museum") || lowerWord.contains("oper") ||
                    lowerWord.contains("saal") || lowerWord.contains("haus")) {
                    
                    // But exclude if it's in the blacklist
                    if (!blacklist.contains(lowerWord)) {
                        logger.debug("  Location: '{}' (from venue keyword)", word);
                        return word;
                    }
                }
            }
        }
        
        // Default location for Vienna events when no specific venue found
        logger.debug("  Location: 'Vienna' (default - no specific venue found)");
        return "Vienna";
    }
    
    /**
     * Validate if a string is a valid venue name
     */
    private boolean isValidVenueName(String location, Set<String> blacklist) {
        if (location == null || location.trim().isEmpty()) {
            return false;
        }
        
        location = location.trim();
        
        // Length check: venue names are typically 3-100 chars
        if (location.length() < 3 || location.length() > 100) {
            return false;
        }
        
        // Check against blacklist
        if (blacklist.contains(location.toLowerCase())) {
            return false;
        }
        
        // Count validation characters
        long periodCount = location.chars().filter(ch -> ch == '.').count();
        long spaceCount = location.chars().filter(ch -> ch == ' ').count();
        long digitCount = location.chars().filter(Character::isDigit).count();
        
        // If it has more than 2 periods, it's likely a sentence
        if (periodCount > 2) {
            return false;
        }
        
        // If it has more than 8 spaces, it's likely a description
        if (spaceCount > 8) {
            return false;
        }
        
        // If it starts with a number or date pattern, it's not a venue
        if (location.matches("^\\d+.*") || location.matches("^(Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember).*")) {
            return false;
        }
        
        // If it contains too many digits (like years), it's probably not a venue
        if (digitCount > 4) {
            return false;
        }
        
        // Exclude URLs and technical strings
        if (location.contains("http://") || location.contains("www.") || 
            location.contains("viewBox") || location.contains("xmlns") ||
            location.contains("svg")) {
            return false;
        }
        
        // Exclude common promotional text patterns
        if (location.toLowerCase().contains("kostenlos") || 
            location.toLowerCase().contains("tickets") ||
            location.toLowerCase().contains("information") ||
            location.toLowerCase().contains("programm") ||
            location.toLowerCase().contains("details")) {
            return false;
        }
        
        // Must have at least one letter
        if (!location.matches(".*[A-Za-zÄÖÜäöüß].*")) {
            return false;
        }
        
        return true;
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
        
        // Filter out common page headings/navigation elements
        String titleLower = event.getTitle().toLowerCase().trim();
        Set<String> pageHeadings = Set.of(
            "eventprogramm", "veranstaltungen", "programm", "events", 
            "kalender", "termine", "übersicht", "alle events",
            "kommende veranstaltungen", "event calendar"
        );
        
        if (pageHeadings.contains(titleLower)) {
            logger.debug("  Filtered out page heading: '{}'", event.getTitle());
            return false;
        }
        
        // Filter out generic promotional/heading phrases
        String[] genericPatterns = {
            "die besten events",
            "die konzert-highlights",
            "die highlights",
            "top events",
            "event-highlights",
            "veranstaltungstipps",
            "unsere empfehlungen",
            "event des monats",
            "empfohlene events",
            "featured events",
            "hier finden sie",
            "weitere informationen",
            "alle informationen",
            "mehr informationen",
            "klicken sie hier",
            "click here",
            "read more",
            "mehr erfahren"
        };
        
        for (String pattern : genericPatterns) {
            if (titleLower.contains(pattern)) {
                logger.debug("  Filtered out generic text: '{}'", event.getTitle());
                return false;
            }
        }
        
        return true;
    }
}
