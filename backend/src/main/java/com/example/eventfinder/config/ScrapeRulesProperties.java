package com.example.eventfinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "scrape-rules")
public class ScrapeRulesProperties {
    private List<RuleConfig> rules = new ArrayList<>();
    private boolean deleteMissingRules = false;

    public List<RuleConfig> getRules() {
        return rules;
    }

    public void setRules(List<RuleConfig> rules) {
        this.rules = rules;
    }

    public boolean isDeleteMissingRules() {
        return deleteMissingRules;
    }

    public void setDeleteMissingRules(boolean deleteMissingRules) {
        this.deleteMissingRules = deleteMissingRules;
    }

    public static class RuleConfig {
        private String siteName;
        private String baseUrl;
        private String eventListSelector;
        private String eventItemSelector;
        private String titleSelector;
        private String dateSelector;
        private String priceSelector;
        private String venueSelector;
        private String linkSelector;
        private String imageSelector;
        private String descriptionSelector;
        private String dateFormat;
        private String dateLocale;
        private String extractionMode;
        private String eventTextPattern;
        private String category;
        private String organizer;
        private Boolean requiresDetailPageFetch;
        private String detailPageSelector;
        private Boolean aiEnabled;
        private String aiPrompt;
        private Integer rateLimitMs;
        private Boolean enabled;
        private String notes;

        public String getSiteName() {
            return siteName;
        }

        public void setSiteName(String siteName) {
            this.siteName = siteName;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getEventListSelector() {
            return eventListSelector;
        }

        public void setEventListSelector(String eventListSelector) {
            this.eventListSelector = eventListSelector;
        }

        public String getEventItemSelector() {
            return eventItemSelector;
        }

        public void setEventItemSelector(String eventItemSelector) {
            this.eventItemSelector = eventItemSelector;
        }

        public String getTitleSelector() {
            return titleSelector;
        }

        public void setTitleSelector(String titleSelector) {
            this.titleSelector = titleSelector;
        }

        public String getDateSelector() {
            return dateSelector;
        }

        public void setDateSelector(String dateSelector) {
            this.dateSelector = dateSelector;
        }

        public String getPriceSelector() {
            return priceSelector;
        }

        public void setPriceSelector(String priceSelector) {
            this.priceSelector = priceSelector;
        }

        public String getVenueSelector() {
            return venueSelector;
        }

        public void setVenueSelector(String venueSelector) {
            this.venueSelector = venueSelector;
        }

        public String getLinkSelector() {
            return linkSelector;
        }

        public void setLinkSelector(String linkSelector) {
            this.linkSelector = linkSelector;
        }

        public String getImageSelector() {
            return imageSelector;
        }

        public void setImageSelector(String imageSelector) {
            this.imageSelector = imageSelector;
        }

        public String getDescriptionSelector() {
            return descriptionSelector;
        }

        public void setDescriptionSelector(String descriptionSelector) {
            this.descriptionSelector = descriptionSelector;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public void setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
        }

        public String getDateLocale() {
            return dateLocale;
        }

        public void setDateLocale(String dateLocale) {
            this.dateLocale = dateLocale;
        }

        public String getExtractionMode() {
            return extractionMode;
        }

        public void setExtractionMode(String extractionMode) {
            this.extractionMode = extractionMode;
        }

        public String getEventTextPattern() {
            return eventTextPattern;
        }

        public void setEventTextPattern(String eventTextPattern) {
            this.eventTextPattern = eventTextPattern;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getOrganizer() {
            return organizer;
        }

        public void setOrganizer(String organizer) {
            this.organizer = organizer;
        }

        public Boolean getRequiresDetailPageFetch() {
            return requiresDetailPageFetch;
        }

        public void setRequiresDetailPageFetch(Boolean requiresDetailPageFetch) {
            this.requiresDetailPageFetch = requiresDetailPageFetch;
        }

        public String getDetailPageSelector() {
            return detailPageSelector;
        }

        public void setDetailPageSelector(String detailPageSelector) {
            this.detailPageSelector = detailPageSelector;
        }

        public Boolean getAiEnabled() {
            return aiEnabled;
        }

        public void setAiEnabled(Boolean aiEnabled) {
            this.aiEnabled = aiEnabled;
        }

        public String getAiPrompt() {
            return aiPrompt;
        }

        public void setAiPrompt(String aiPrompt) {
            this.aiPrompt = aiPrompt;
        }

        public Integer getRateLimitMs() {
            return rateLimitMs;
        }

        public void setRateLimitMs(Integer rateLimitMs) {
            this.rateLimitMs = rateLimitMs;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
