package com.example.eventfinder.scraper.util;

import com.example.eventfinder.scraper.ScrapedEvent;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utilities for extracting pricing information from HTML/text.
 * Handles various price formats: "€ 15", "ab € 14,70", "€ 8/10", "frei", etc.
 */
public class PriceExtractor {
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+[.,]?\\d*)");
    
    /**
     * Parse pricing information from a string.
     * Sets priceFrom, priceTo, and priceNote on the event.
     */
    public static void extractPrice(ScrapedEvent event, String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            event.setPriceNote("tba");
            return;
        }
        
        priceStr = priceStr.trim();
        event.setPriceNote(priceStr);
        
        // Free events
        if (priceStr.matches("(?i).*(frei|free|kostenlos|gratis|FREI|FREIER|EINTRITT).*")) {
            event.setPriceFrom(BigDecimal.ZERO);
            return;
        }
        
        // Extract numeric prices
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
     * Extract price from an HTML element.
     */
    public static void extractPriceFromElement(ScrapedEvent event, Element element) {
        if (element == null) {
            return;
        }
        extractPrice(event, safeText(element));
    }
    
    /**
     * Safely extract text from an element, handling nulls.
     */
    private static String safeText(Element element) {
        if (element == null) {
            return null;
        }
        String text = element.text();
        return text.isEmpty() ? null : text;
    }
}
