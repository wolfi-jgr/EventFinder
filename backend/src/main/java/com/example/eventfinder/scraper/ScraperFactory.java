package com.example.eventfinder.scraper;

import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.scraper.impl.CssSelectorScraper;
import com.example.eventfinder.scraper.impl.HybridScraper;
import com.example.eventfinder.scraper.impl.RegexScraper;
import com.example.eventfinder.scraper.impl.WordPressScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for selecting the appropriate scraper based on ScrapeRule configuration.
 * Implements a priority order: Hybrid > WordPress > Regex > CSS Selector (default).
 */
@Component
public class ScraperFactory {
    private static final Logger logger = LoggerFactory.getLogger(ScraperFactory.class);
    
    private final WordPressScraper wordPressScraper;
    private final CssSelectorScraper cssSelectorScraper;
    private final RegexScraper regexScraper;
    private final HybridScraper hybridScraper;
    
    public ScraperFactory(
            WordPressScraper wordPressScraper,
            CssSelectorScraper cssSelectorScraper,
            RegexScraper regexScraper,
            HybridScraper hybridScraper) {
        this.wordPressScraper = wordPressScraper;
        this.cssSelectorScraper = cssSelectorScraper;
        this.regexScraper = regexScraper;
        this.hybridScraper = hybridScraper;
    }
    
    /**
     * Select the appropriate scraper for the given rule.
     * Priority: Hybrid (auto-detect) > WordPress > Regex > CSS Selector
     *
     * @param rule The scrape rule configuration
     * @return The selected scraper
     */
    public EventScraper getScraper(ScrapeRule rule) {
        String extractionMode = rule.getExtractionMode() != null 
            ? rule.getExtractionMode() 
            : "CSS_SELECTOR";
        
        // Hybrid mode: try WordPress first, fall back to configured mode
        if ("HYBRID".equals(extractionMode) || "AUTO".equals(extractionMode)) {
            logger.debug("Using HYBRID scraper for {}", rule.getSiteName());
            return hybridScraper;
        }
        
        // WordPress API mode
        if ("WORDPRESS".equals(extractionMode)) {
            logger.debug("Using WORDPRESS scraper for {}", rule.getSiteName());
            return wordPressScraper;
        }
        
        // Regex mode
        if ("REGEX".equals(extractionMode)) {
            logger.debug("Using REGEX scraper for {}", rule.getSiteName());
            return regexScraper;
        }
        
        // CSS Selector mode (default)
        logger.debug("Using CSS_SELECTOR scraper for {}", rule.getSiteName());
        return cssSelectorScraper;
    }
    
    /**
     * Get all available scrapers (for debugging/monitoring).
     */
    public List<EventScraper> getAllScrapers() {
        List<EventScraper> scrapers = new ArrayList<>();
        scrapers.add(hybridScraper);
        scrapers.add(wordPressScraper);
        scrapers.add(regexScraper);
        scrapers.add(cssSelectorScraper);
        return scrapers;
    }
}
