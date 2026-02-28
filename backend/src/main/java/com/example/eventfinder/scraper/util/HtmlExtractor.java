package com.example.eventfinder.scraper.util;

import org.jsoup.nodes.Element;

/**
 * Shared utilities for safe HTML element access.
 */
public class HtmlExtractor {
    
    /**
     * Safely get text from an element, handling nulls.
     */
    public static String safeText(Element element) {
        if (element == null) {
            return null;
        }
        String text = element.text().trim();
        return text.isEmpty() ? null : text;
    }
    
    /**
     * Safely get attribute from an element, handling nulls.
     */
    public static String safeAttr(Element element, String attrName) {
        if (element == null) {
            return null;
        }
        String attr = element.attr(attrName);
        return attr == null || attr.isEmpty() ? null : attr;
    }
    
    /**
     * Check if venue name is valid (not too short, not blacklisted, etc.).
     */
    public static boolean isValidVenueName(String venue) {
        if (venue == null || venue.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = venue.trim();
        
        // Length check: venue names are typically 3-100 chars
        if (trimmed.length() < 3 || trimmed.length() > 100) {
            return false;
        }
        
        // Check against blacklist
        java.util.Set<String> LOCATION_BLACKLIST = java.util.Set.of(
            "kostenlos", "programm", "eventprogramm", "informationen", "details", "tickets"
        );
        
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
