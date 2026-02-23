package com.example.eventfinder.scraper.impl;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.scraper.BaseWebScraper;
import com.example.eventfinder.scraper.ScrapedEvent;
import com.example.eventfinder.scraper.ScraperConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic rule-based scraper that works for multiple websites.
 * Uses ScrapeRule configurations to extract event data.
 */
@Component
public class GenericScraper extends BaseWebScraper {
    private static final String PARSER_VERSION = "GenericScraper_v1";
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+[.,]?\\d*)");
    private static final Map<String, Integer> GERMAN_MONTHS = Map.ofEntries(
        Map.entry("januar", 1),
        Map.entry("jaenner", 1),
        Map.entry("februar", 2),
        Map.entry("feber", 2),
        Map.entry("maerz", 3),
        Map.entry("marz", 3),
        Map.entry("april", 4),
        Map.entry("mai", 5),
        Map.entry("juni", 6),
        Map.entry("juli", 7),
        Map.entry("august", 8),
        Map.entry("september", 9),
        Map.entry("oktober", 10),
        Map.entry("november", 11),
        Map.entry("dezember", 12)
    );
    
    // Location blacklist - common non-venue words
    private static final Set<String> LOCATION_BLACKLIST = Set.of(
        "location", "ort", "venue", "veranstaltungsort", "address", 
        "wien", "vienna", "theater", "kino", "museum", "kostenlos",
        "programm", "eventprogramm", "informationen", "details", "tickets"
    );
    
    // German day names for parsing
    private static final Map<String, Integer> GERMAN_DAYS = Map.ofEntries(
        Map.entry("montag", 1),
        Map.entry("dienstag", 2),
        Map.entry("mittwoch", 3),
        Map.entry("donnerstag", 4),
        Map.entry("freitag", 5),
        Map.entry("samstag", 6),
        Map.entry("sonntag", 7),
        Map.entry("mo", 1),
        Map.entry("di", 2),
        Map.entry("mi", 3),
        Map.entry("do", 4),
        Map.entry("fr", 5),
        Map.entry("sa", 6),
        Map.entry("so", 7)
    );
    
    public GenericScraper(ScraperConfig config) {
        super(config);
    }
    
    @Override
    public String getScraperName() {
        return "GenericScraper";
    }
    
    @Override
    public String getTargetDomain() {
        return "generic";
    }

    /**
     * Scrape events using a rule configuration
     */
    public List<Event> scrapeWithRule(ScrapeRule rule) throws Exception {
        if (!rule.getEnabled()) {
            throw new IllegalStateException("Scrape rule for " + rule.getSiteName() + " is disabled");
        }
        
        // Try WordPress API first - always attempt for any site
        if (isWordPressSite(rule.getBaseUrl())) {
            logger.info("[{}] WordPress site detected for {}. Attempting REST API scraping...", 
                getScraperName(), rule.getSiteName());
            try {
                List<Event> events = scrapeFromWordPressAPI(rule);
                if (!events.isEmpty()) {
                    logger.info("[{}] Successfully scraped {} events from WordPress API", 
                        getScraperName(), events.size());
                    return events;
                }
            } catch (Exception e) {
                logger.warn("[{}] WordPress API scraping failed, falling back to HTML fetch: {}", 
                    getScraperName(), e.getMessage());
            }
        }
        
        return scrapeEvents(rule.getBaseUrl(), rule);
    }
    
    /**
     * Scrape events from cached/pre-fetched HTML
     */
    public List<Event> scrapeWithRule(ScrapeRule rule, String cachedHtml) throws Exception {
        if (!rule.getEnabled()) {
            throw new IllegalStateException("Scrape rule for " + rule.getSiteName() + " is disabled");
        }
        
        // Try WordPress API first - prefer fresh API data even if cached HTML exists
        if (isWordPressSite(rule.getBaseUrl())) {
            logger.info("[{}] WordPress site detected for {}. Attempting REST API (ignoring cache)...", 
                getScraperName(), rule.getSiteName());
            try {
                List<Event> events = scrapeFromWordPressAPI(rule);
                if (!events.isEmpty()) {
                    logger.info("[{}] Successfully scraped {} events from WordPress API (bypassed cache)", 
                        getScraperName(), events.size());
                    return events;
                }
            } catch (Exception e) {
                logger.warn("[{}] WordPress API scraping failed, falling back to cached HTML: {}", 
                    getScraperName(), e.getMessage());
            }
        }
        
        // Parse cached HTML as fallback
        Document doc = org.jsoup.Jsoup.parse(cachedHtml, rule.getBaseUrl());
        return scrapeEventsFromDocument(doc, rule);
    }
    
    @Override
    public List<Event> scrapeEvents(String url) throws Exception {
        // This shouldn't be called directly; use scrapeWithRule instead
        throw new UnsupportedOperationException("Use scrapeWithRule(ScrapeRule) instead");
    }

    /**
     * Internal method that scrapes with a given rule
     */
    private List<Event> scrapeEvents(String url, ScrapeRule rule) throws Exception {
        // Document doc = fetchDocument(url);
        // Check if this site needs JavaScript rendering (is JavaScript-heavy)
        if (isJavaScriptHeavySite(rule.getSiteName())) {
            logger.info("[{}] Detected JavaScript-heavy site: {}. Using Playwright for rendering...", 
                getScraperName(), rule.getSiteName());
            try {
                Document doc = fetchDocumentWithPlaywright(url);
                return scrapeEventsFromDocument(doc, rule);
            } catch (Exception e) {
                logger.warn("[{}] Playwright rendering failed for {}, falling back to standard fetch: {}", 
                    getScraperName(), rule.getSiteName(), e.getMessage());
                // Fall back to standard fetch
                Document doc = fetchDocument(url);
                return scrapeEventsFromDocument(doc, rule);
            }
        } else {
            // Regular fetch for non-JavaScript-heavy sites
            Document doc = fetchDocument(url);
            return scrapeEventsFromDocument(doc, rule);
        }
    }
    
    /**
     * Extract events from a document using rule
     * Falls back to CSS selectors if WordPress API wasn't used (handled at scrapeWithRule level)
     */
    private List<Event> scrapeEventsFromDocument(Document doc, ScrapeRule rule) throws Exception {
        List<Event> events = new ArrayList<>();
        String rawHtml = doc.html();
        
        // Choose extraction strategy based on rule mode
        if ("REGEX".equals(rule.getExtractionMode()) && rule.getEventTextPattern() != null) {
            // Regex-based extraction (for text-heavy pages)
            events = extractWithRegex(doc, rule, rawHtml);
        } else {
            // CSS Selector-based extraction (default)
            events = extractWithSelectors(doc, rule, rawHtml);
        }
        
        logger.info("[{}] Scraped {} events from {} using rule: {}", 
            getScraperName(), events.size(), rule.getBaseUrl(), rule.getSiteName());
        
        return events;
    }
    
    /**
     * Extract events using CSS selectors from rule
     */
    private List<Event> extractWithSelectors(Document doc, ScrapeRule rule, String rawHtml) {
        List<Event> events = new ArrayList<>();
        
        // Find event items using configured selector
        Elements eventItems = doc.select(rule.getEventItemSelector());
        
        for (Element item : eventItems) {
            try {
                ScrapedEvent scraped = new ScrapedEvent();

                String linkHref = null;
                if (rule.getLinkSelector() != null) {
                    Element linkEl = item.selectFirst(rule.getLinkSelector());
                    linkHref = safeAttr(linkEl, "href");
                    if (linkHref != null && !linkHref.startsWith("http")) {
                        linkHref = new java.net.URL(new java.net.URL(rule.getBaseUrl()), linkHref).toString();
                    }
                }
                
                // Extract title
                if (rule.getTitleSelector() != null) {
                    Element titleEl = item.selectFirst(rule.getTitleSelector());
                    scraped.setTitle(safeText(titleEl));
                }
                
                // Extract date/time
                if (rule.getDateSelector() != null) {
                    Element dateEl = item.selectFirst(rule.getDateSelector());
                    String dateStr = safeText(dateEl);
                    LocalDateTime dateTime = parseDateTime(dateStr, rule.getDateFormat(), rule.getDateLocale());
                    if (dateTime == null && linkHref != null) {
                        dateTime = parseDateFromUrl(linkHref);
                    }
                    scraped.setStartDateTime(dateTime);
                }
                
                // Extract price
                if (rule.getPriceSelector() != null) {
                    Element priceEl = item.selectFirst(rule.getPriceSelector());
                    String priceStr = safeText(priceEl);
                    parsePricing(scraped, priceStr);
                }
                
                // Extract venue
                if (rule.getVenueSelector() != null) {
                    Element venueEl = item.selectFirst(rule.getVenueSelector());
                    String venue = safeText(venueEl);
                    // Validate and clean venue name
                    if (venue != null && isValidVenueName(venue)) {
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
                    scraped.setImageUrl(safeAttr(imgEl, "src"));
                }
                
                // Set defaults from rule
                scraped.setCategory(rule.getCategory());
                scraped.setOrganizer(rule.getOrganizer());
                scraped.setRawHtml(rawHtml);
                
                // Only add if we have at least title and date
                if (scraped.getTitle() != null && scraped.getStartDateTime() != null) {
                    Event event = convertToEvent(scraped, rule);
                    events.add(event);
                }
                
            } catch (Exception e) {
                logger.warn("Failed to parse event item: {}", e.getMessage());
            }
        }
        
        return events;
    }

    /**
     * Extract events using regex pattern from rule
     */
    private List<Event> extractWithRegex(Document doc, ScrapeRule rule, String rawHtml) {
        List<Event> events = new ArrayList<>();
        
        String text = doc.text();
        Pattern pattern = Pattern.compile(rule.getEventTextPattern());
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            try {
                ScrapedEvent scraped = new ScrapedEvent();
                
                // Extract groups from regex
                if (matcher.groupCount() >= 1) {
                    scraped.setTitle(matcher.group(1));
                }
                if (matcher.groupCount() >= 2) {
                    String dateStr = matcher.group(2);
                    LocalDateTime dateTime = parseDateTime(dateStr, rule.getDateFormat(), rule.getDateLocale());
                    scraped.setStartDateTime(dateTime);
                }
                if (matcher.groupCount() >= 3) {
                    parsePricing(scraped, matcher.group(3));
                }
                
                scraped.setCategory(rule.getCategory());
                scraped.setOrganizer(rule.getOrganizer());
                scraped.setRawHtml(rawHtml);
                scraped.setSourceUrl(rule.getBaseUrl());
                
                if (scraped.getTitle() != null && scraped.getStartDateTime() != null) {
                    Event event = convertToEvent(scraped, rule);
                    events.add(event);
                }
                
            } catch (Exception e) {
                logger.warn("Failed to parse regex match: {}", e.getMessage());
            }
        }
        
        return events;
    }

    /**
     * Check if a site requires JavaScript rendering (is JavaScript-heavy)
     */
    private boolean isJavaScriptHeavySite(String siteName) {
        // List of sites that are known to render events using JavaScript
        Set<String> jsHeavySites = Set.of(
            "savedate.io/@prst",
            "savedate.io",
            "eventsfinder",
            "react-events"
        );
        
        boolean isJsHeavy = jsHeavySites.stream()
            .anyMatch(site -> siteName.toLowerCase().contains(site.toLowerCase()));
        logger.info("[{}] JavaScript-heavy check for '{}': {}", getScraperName(), siteName, isJsHeavy);
        return isJsHeavy;
    }

    /**
     * Fetch document using Playwright for JavaScript-heavy sites
     * Waits for content to load before returning HTML
     */
    private Document fetchDocumentWithPlaywright(String url) throws Exception {
        logger.info("[{}] Using Playwright to fetch: {}", getScraperName(), url);
        
        try (Playwright playwright = Playwright.create()) {
            // Use chromium browser
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(true);
            
            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                BrowserContext context = browser.newContext();
                Page page = context.newPage();
                
                try {
                    // Navigate to the page - wait for initial response
                    page.navigate(url);
                    
                    // Wait for content to load - give JavaScript time to render events
                    page.waitForTimeout(3000);
                    
                    // Get the rendered HTML
                    String content = page.content();
                    
                    logger.info("[{}] Successfully fetched and rendered: {}", getScraperName(), url);
                    
                    // Parse as JSoup Document
                    return Jsoup.parse(content, url);
                    
                } finally {
                    page.close();
                    context.close();
                }
            }
        }
    }

    /**
     * Check if a site is a WordPress site by testing for REST API availability
     */
    private boolean isWordPressSite(String baseUrl) {
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
                getScraperName(), baseUrl, e.getMessage());
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
        
        logger.info("[{}] Fetching from WordPress API: {}", getScraperName(), wpJsonUrl);
        
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
                    logger.warn("[{}] Failed to parse WordPress post: {}", getScraperName(), e.getMessage());
                }
            }
        }
        
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
        
        // Extract date
        if (post.has("date")) {
            try {
                String dateStr = post.get("date").asText();
                // WordPress returns ISO 8601 format: 2026-02-18T14:11:29
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, 
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                scraped.setStartDateTime(dateTime);
            } catch (Exception e) {
                logger.debug("[{}] Failed to parse WordPress date: {}", getScraperName(), e.getMessage());
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
        
        return convertToEvent(scraped, rule);
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
            if (isValidVenueName(venue)) {
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
                parsePricing(scraped, priceStr);
            }
        }
    }

    /**
     * Parse date/time string using format and locale from rule
     */
    private LocalDateTime parseDateTime(String dateStr, String format, String locale) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        String cleaned = dateStr.replace('\u00A0', ' ').trim();
        cleaned = cleaned.replace('–', '-').replace('—', '-');
        cleaned = cleaned.replace("Jänner", "Januar").replace("Feber", "Februar");
        cleaned = cleaned.replace("MÃ¤rz", "März").replace("M├ñrz", "März");
        Locale parsedLocale = locale != null ? Locale.forLanguageTag(locale) : Locale.getDefault();

        try {
            Matcher timeMatcher = Pattern.compile("(\\d{1,2}:\\d{2})").matcher(cleaned);
            String timePart = timeMatcher.find() ? timeMatcher.group(1) : null;
            
            // Try parsing German day names (e.g., "Montag, 19:30 Uhr")
            LocalDateTime dayNameDate = parseGermanDayName(cleaned, timePart);
            if (dayNameDate != null) {
                return dayNameDate;
            }

            LocalDateTime looseSlashDate = parseLooseSlashDate(cleaned, timePart);
            if (looseSlashDate != null) {
                return looseSlashDate;
            }

            Matcher shortDateAny = Pattern.compile("(\\d{1,2})/(\\d{1,2})").matcher(cleaned);
            if (shortDateAny.find()) {
                int day = Integer.parseInt(shortDateAny.group(1));
                int month = Integer.parseInt(shortDateAny.group(2));
                int year = LocalDate.now().getYear();
                LocalDate date = LocalDate.of(year, month, day);
                LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                return LocalDateTime.of(date, time);
            }

            Matcher namedDateAny = Pattern.compile("(\\d{1,2})\\.\\s*([\\p{L}]+)(?:\\s*(\\d{4}))?").matcher(cleaned);
            String day = null;
            String monthName = null;
            String year = null;
            while (namedDateAny.find()) {
                day = namedDateAny.group(1);
                monthName = namedDateAny.group(2);
                year = namedDateAny.group(3);
            }

            if (day != null && monthName != null) {
                String resolvedYear = year != null ? year : String.valueOf(LocalDate.now().getYear());
                String normalizedMonth = normalizeMonthName(monthName);
                Integer month = GERMAN_MONTHS.get(normalizedMonth);
                if (month != null) {
                    LocalDate date = LocalDate.of(Integer.parseInt(resolvedYear), month, Integer.parseInt(day));
                    LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                    return LocalDateTime.of(date, time);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse date '{}' with format '{}': {}", cleaned, format, e.getMessage());
        }

        try {
            if (format != null && !format.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withLocale(parsedLocale);
                return LocalDateTime.parse(cleaned, formatter);
            }
            // Try ISO format as fallback
            return LocalDateTime.parse(cleaned);
        } catch (DateTimeParseException e) {
            // Fall through to flexible parsing below.
        }

        try {
            String cleanedForDate = cleaned.replaceAll("^[^0-9]+", "");
            if (cleanedForDate.isEmpty()) {
                cleanedForDate = cleaned;
            }

            Matcher leadingShortDate = Pattern.compile("^(\\d{1,2})/(\\d{1,2})").matcher(cleanedForDate);
            if (leadingShortDate.find()) {
                String resolvedYear = String.valueOf(LocalDate.now().getYear());
                String dateToParse = leadingShortDate.group(1) + "/" + leadingShortDate.group(2) + "/" + resolvedYear;
                LocalDate date = LocalDate.parse(dateToParse, DateTimeFormatter.ofPattern("d/M/yyyy"));
                return LocalDateTime.of(date, LocalTime.MIDNIGHT);
            }

            String timePart = null;
            Matcher timeMatcher = Pattern.compile("(\\d{1,2}:\\d{2})").matcher(cleanedForDate);
            if (timeMatcher.find()) {
                timePart = timeMatcher.group(1);
            }

            String datePart = null;
            Matcher numericDateMatcher = Pattern.compile("(\\d{1,2}[./]\\d{1,2}[./]\\d{2,4})").matcher(cleanedForDate);
            while (numericDateMatcher.find()) {
                datePart = numericDateMatcher.group(1);
            }

            if (datePart != null) {
                String normalizedDate = datePart.replace('/', '.');
                String[] parts = normalizedDate.split("\\.");
                if (parts.length == 3) {
                    String year = parts[2];
                    if (year.length() == 2) {
                        year = "20" + year;
                    }
                    String dateToParse = parts[0] + "." + parts[1] + "." + year;
                    LocalDate date = LocalDate.parse(dateToParse, DateTimeFormatter.ofPattern("d.M.yyyy"));
                    LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                    return LocalDateTime.of(date, time);
                }
            }

            Matcher namedDateMatcher = Pattern.compile("(\\d{1,2})\\.\\s*([\\p{L}]+)(?:\\s*(\\d{4}))?").matcher(cleanedForDate);
            String day = null;
            String monthName = null;
            String year = null;
            while (namedDateMatcher.find()) {
                day = namedDateMatcher.group(1);
                monthName = namedDateMatcher.group(2);
                year = namedDateMatcher.group(3);
            }

            if (day != null && monthName != null) {
                String resolvedYear = year != null ? year : String.valueOf(LocalDate.now().getYear());
                String normalizedMonth = normalizeMonthName(monthName);
                Integer month = GERMAN_MONTHS.get(normalizedMonth);
                LocalDate date;
                if (month != null) {
                    date = LocalDate.of(Integer.parseInt(resolvedYear), month, Integer.parseInt(day));
                } else {
                    String dateToParse = day + ". " + monthName + " " + resolvedYear;
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", parsedLocale);
                    date = LocalDate.parse(dateToParse, formatter);
                }
                LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                return LocalDateTime.of(date, time);
            }

            Matcher shortDateMatcher = Pattern.compile("(\\d{1,2})/(\\d{1,2})").matcher(cleanedForDate);
            if (shortDateMatcher.find()) {
                String dayPart = shortDateMatcher.group(1);
                String monthPart = shortDateMatcher.group(2);
                String resolvedYear = String.valueOf(LocalDate.now().getYear());
                String dateToParse = dayPart + "/" + monthPart + "/" + resolvedYear;
                LocalDate date = LocalDate.parse(dateToParse, DateTimeFormatter.ofPattern("d/M/yyyy"));
                LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                return LocalDateTime.of(date, time);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse date '{}' with format '{}': {}", cleaned, format, e.getMessage());
        }

        logger.warn("Failed to parse date '{}' with format '{}'", cleaned, format);
        return null;
    }

    private String normalizeMonthName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        normalized = normalized
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss");
        return normalized.replaceAll("[^a-z]", "");
    }

    private LocalDateTime parseLooseSlashDate(String value, String timePart) {
        int year = LocalDate.now().getYear();
        for (int i = 0; i + 4 < value.length(); i++) {
            char d1 = value.charAt(i);
            char d2 = value.charAt(i + 1);
            char sep = value.charAt(i + 2);
            char m1 = value.charAt(i + 3);
            char m2 = value.charAt(i + 4);

            if (Character.isDigit(d1) && Character.isDigit(d2) && sep == '/' && Character.isDigit(m1) && Character.isDigit(m2)) {
                int day = Integer.parseInt("" + d1 + d2);
                int month = Integer.parseInt("" + m1 + m2);
                LocalDate date = LocalDate.of(year, month, day);
                LocalTime time = timePart != null ? LocalTime.parse(timePart) : LocalTime.MIDNIGHT;
                return LocalDateTime.of(date, time);
            }
        }
        return null;
    }

    private LocalDateTime parseDateFromUrl(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("/(\\d{2})(\\d{2})-").matcher(url);
        if (matcher.find()) {
            int day = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int year = LocalDate.now().getYear();
            return LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIDNIGHT);
        }
        return null;
    }
    
    /**
     * Parse pricing information
     * Examples: "Frei", "€ freie Spende", "ab € 14,70", "€ 15", "€ 8/10", "€ 1/6/9/12"
     */
    private void parsePricing(ScrapedEvent event, String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            event.setPriceNote("tba");
            return;
        }
        
        priceStr = priceStr.trim();
        event.setPriceNote(priceStr);
        
        // Free events
        if (priceStr.matches("(?i).*(frei|free|kostenlos).*")) {
            event.setPriceFrom(BigDecimal.ZERO);
            return;
        }
        
        // Extract numeric prices from strings like "€ 15", "ab € 14,70", "€ 8/10", "€ 1/6/9/12"
        Matcher matcher = PRICE_PATTERN.matcher(priceStr);
        
        List<BigDecimal> prices = new ArrayList<>();
        while (matcher.find()) {
            String numStr = matcher.group(1).replace(",", ".");
            try {
                prices.add(new BigDecimal(numStr));
            } catch (NumberFormatException e) {
                // Skip invalid numbers
            }
        }
        
        if (!prices.isEmpty()) {
            Collections.sort(prices);
            event.setPriceFrom(prices.get(0));
            if (prices.size() > 1) {
                event.setPriceTo(prices.get(prices.size() - 1));
            }
        }
    }
    
    /**
     * Convert ScrapedEvent to Event entity with rule metadata
     */
    private Event convertToEvent(ScrapedEvent scraped, ScrapeRule rule) {
        Event event = new Event();
        
        // Basic fields
        event.setTitle(scraped.getTitle());
        event.setDescription(scraped.getDescription());
        event.setStartDateTime(scraped.getStartDateTime());
        event.setEndDateTime(scraped.getEndDateTime());
        event.setLocation(scraped.getLocation());
        event.setLatitude(scraped.getLatitude());
        event.setLongitude(scraped.getLongitude());
        event.setCategory(scraped.getCategory());
        event.setImageUrl(scraped.getImageUrl());
        event.setOrganizer(scraped.getOrganizer());
        event.setSourceUrl(scraped.getSourceUrl());
        
        // Pricing
        event.setPriceFrom(scraped.getPriceFrom());
        event.setPriceTo(scraped.getPriceTo());
        event.setPriceNote(scraped.getPriceNote());
        
        // Venue/room info
        event.setVenue(scraped.getVenue());
        
        // Recurrence
        event.setIsRecurring(scraped.getIsRecurring() != null ? scraped.getIsRecurring() : false);
        event.setRecurringPattern(scraped.getRecurringPattern());
        
        // Tags
        event.setTags(scraped.getTags());
        
        // Raw HTML for offline re-parsing (truncate if too long)
        String rawHtml = scraped.getRawHtml();
        if (rawHtml != null && rawHtml.length() > 50000) {
            rawHtml = rawHtml.substring(0, 50000);
        }
        event.setRawSourceHtml(rawHtml);
        
        // Scraper metadata
        setScraperMetadata(event, rule.getSiteName(), PARSER_VERSION);
        
        // Deduplication hash
        event.setDeduplicationHash(
            calculateDeduplicationHash(event.getTitle(), event.getStartDateTime(), event.getOrganizer())
        );
        
        return event;
    }
    
    /**
     * Parse German day name and return LocalDateTime for next occurrence
     * e.g., "Montag, 19:30 Uhr" -> next Monday at 19:30
     */
    private LocalDateTime parseGermanDayName(String text, String timePart) {
        if (text == null) return null;
        
        String lower = text.toLowerCase()
            .replace(",", "")
            .replace(".", "")
            .replace(" uhr", "")
            .trim();
        
        // Find day name in text
        for (Map.Entry<String, Integer> entry : GERMAN_DAYS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                int targetDayOfWeek = entry.getValue();
                LocalDate today = LocalDate.now();
                LocalDate targetDate = today;
                
                // Find next occurrence of this day (including today if it matches)
                while (targetDate.getDayOfWeek().getValue() != targetDayOfWeek) {
                    targetDate = targetDate.plusDays(1);
                    // Safety limit - don't search beyond 7 days
                    if (targetDate.isAfter(today.plusDays(7))) {
                        return null;
                    }
                }
                
                // Parse time or default to 19:00
                LocalTime time = LocalTime.of(19, 0);
                if (timePart != null) {
                    try {
                        time = LocalTime.parse(timePart);
                    } catch (Exception e) {
                        // Keep default time
                    }
                }
                
                return LocalDateTime.of(targetDate, time);
            }
        }
        
        return null;
    }
    
    /**
     * Validate if a string is a valid venue name
     */
    private boolean isValidVenueName(String venue) {
        if (venue == null || venue.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = venue.trim();
        
        // Length check: venue names are typically 3-100 chars
        if (trimmed.length() < 3 || trimmed.length() > 100) {
            return false;
        }
        
        // Check against blacklist
        String lower = trimmed.toLowerCase();
        if (LOCATION_BLACKLIST.contains(lower)) {
            return false;
        }
        
        // Must have at least one letter
        if (!trimmed.matches(".*[A-Za-zÄÖÜäöüß].*")) {
            return false;
        }
        
        return true;
    }
}
