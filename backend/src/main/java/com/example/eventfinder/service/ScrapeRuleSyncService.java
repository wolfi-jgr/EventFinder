package com.example.eventfinder.service;

import com.example.eventfinder.config.ScrapeRulesProperties;
import com.example.eventfinder.model.ScrapeRule;
import com.example.eventfinder.repository.ScrapeRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ScrapeRuleSyncService implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(ScrapeRuleSyncService.class);

    private final ScrapeRuleRepository scrapeRuleRepository;
    private final ScrapeRulesProperties scrapeRulesProperties;

    public ScrapeRuleSyncService(
            ScrapeRuleRepository scrapeRuleRepository,
            ScrapeRulesProperties scrapeRulesProperties) {
        this.scrapeRuleRepository = scrapeRuleRepository;
        this.scrapeRulesProperties = scrapeRulesProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        syncNow();
    }

    @Transactional
    public Map<String, Object> syncNow() {
        List<ScrapeRulesProperties.RuleConfig> configuredRules = scrapeRulesProperties.getRules();
        Map<String, Object> result = new HashMap<>();

        if (configuredRules == null || configuredRules.isEmpty()) {
            logger.warn("No scrape-rules.rules configured. Check scrape-rules.yml. Skipping scrape rule sync.");
            result.put("inserted", 0);
            result.put("updated", 0);
            result.put("deleted", 0);
            result.put("configuredRules", 0);
            result.put("message", "No configured rules found in scrape-rules.yml");
            result.put("timestamp", LocalDateTime.now());
            return result;
        }

        int inserted = 0;
        int updated = 0;
        Set<String> configuredSiteNames = new HashSet<>();

        for (ScrapeRulesProperties.RuleConfig configuredRule : configuredRules) {
            if (configuredRule.getSiteName() == null || configuredRule.getSiteName().isBlank()) {
                logger.warn("Skipping scraper rule with empty siteName");
                continue;
            }
            configuredSiteNames.add(configuredRule.getSiteName());

            ScrapeRule existingRule = scrapeRuleRepository.findBySiteName(configuredRule.getSiteName())
                    .orElseGet(ScrapeRule::new);

            boolean isNew = existingRule.getId() == null;
            if (isNew) {
                existingRule.setCreatedAt(LocalDateTime.now());
            }

            applyConfig(existingRule, configuredRule);
            existingRule.setUpdatedAt(LocalDateTime.now());
            scrapeRuleRepository.save(existingRule);

            if (isNew) {
                inserted++;
            } else {
                updated++;
            }
        }

        int deleted = 0;
        if (scrapeRulesProperties.isDeleteMissingRules()) {
            List<ScrapeRule> currentDbRules = scrapeRuleRepository.findAll();
            for (ScrapeRule currentDbRule : currentDbRules) {
                if (!configuredSiteNames.contains(currentDbRule.getSiteName())) {
                    scrapeRuleRepository.delete(currentDbRule);
                    deleted++;
                }
            }
        }

        logger.info("Scrape rule sync complete: {} inserted, {} updated, {} deleted", inserted, updated, deleted);

        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("deleted", deleted);
        result.put("configuredRules", configuredRules.size());
        result.put("deleteMissingRules", scrapeRulesProperties.isDeleteMissingRules());
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    private void applyConfig(ScrapeRule target, ScrapeRulesProperties.RuleConfig source) {
        target.setSiteName(source.getSiteName());
        target.setBaseUrl(source.getBaseUrl());
        target.setEventListSelector(source.getEventListSelector());
        target.setEventItemSelector(source.getEventItemSelector());
        target.setTitleSelector(source.getTitleSelector());
        target.setDateSelector(source.getDateSelector());
        target.setPriceSelector(source.getPriceSelector());
        target.setVenueSelector(source.getVenueSelector());
        target.setLinkSelector(source.getLinkSelector());
        target.setImageSelector(source.getImageSelector());
        target.setDescriptionSelector(source.getDescriptionSelector());
        target.setDateFormat(source.getDateFormat());
        target.setDateLocale(source.getDateLocale());
        target.setExtractionMode(defaultString(source.getExtractionMode(), "CSS_SELECTOR"));
        target.setEventTextPattern(source.getEventTextPattern());
        target.setCategory(source.getCategory());
        target.setOrganizer(source.getOrganizer());
        target.setRequiresDetailPageFetch(defaultBoolean(source.getRequiresDetailPageFetch(), false));
        target.setDetailPageSelector(source.getDetailPageSelector());
        target.setAiEnabled(defaultBoolean(source.getAiEnabled(), false));
        target.setAiPrompt(source.getAiPrompt());
        target.setRateLimitMs(source.getRateLimitMs());
        target.setEnabled(defaultBoolean(source.getEnabled(), true));
        target.setNotes(source.getNotes());
    }

    private boolean defaultBoolean(Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    private String defaultString(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
