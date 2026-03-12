package com.example.eventfinder.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "end_time")
    private LocalTime endTime;

    private String location;

    private Double latitude;

    private Double longitude;

    private String category;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "image_url")
    private String imageUrl;

    private String organizer;

    // Legacy price field (deprecated in favor of priceFrom/priceTo)
    private String price;

    // New pricing fields for range support
    @Column(name = "price_from")
    private BigDecimal priceFrom;

    @Column(name = "price_to")
    private BigDecimal priceTo;

    @Column(name = "price_note", length = 500)
    private String priceNote; // e.g., "Free Spende", "tba", "ab €14,70"

    // Venue/room information
    private String venue; // Physical venue name or room

    // Recurrence tracking
    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column(name = "recurring_pattern")
    private String recurringPattern; // e.g., "WEEKLY_WEDNESDAY"

    // Scraper metadata
    @Column(name = "scraped_from", length = 100)
    private String scrapedFrom; // e.g., "theloft.at", "grelleforelle.com"

    @Column(name = "parser_version", length = 50)
    private String parserVersion; // e.g., "TheLoftParser_v1"

    @Column(name = "dedup_hash", length = 64)
    private String deduplicationHash; // SHA256 for duplicate detection

    // Raw HTML storage for offline re-parsing
    @Column(name = "raw_source_html", length = 50000)
    private String rawSourceHtml;

    // Tags for categorization
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_tags", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "tag")
    private java.util.Set<String> tags = new java.util.HashSet<>();

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    // Constructors
    public Event() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    @Transient
    public LocalDateTime getStartDateTime() {
        if (startDate == null) {
            return null;
        }
        return LocalDateTime.of(startDate, startTime != null ? startTime : LocalTime.MIDNIGHT);
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        if (startDateTime == null) {
            this.startDate = null;
            this.startTime = null;
            return;
        }
        this.startDate = startDateTime.toLocalDate();
        this.startTime = startDateTime.toLocalTime();
    }

    @Transient
    public LocalDateTime getEndDateTime() {
        if (endDate == null) {
            return null;
        }
        return LocalDateTime.of(endDate, endTime != null ? endTime : LocalTime.MIDNIGHT);
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        if (endDateTime == null) {
            this.endDate = null;
            this.endTime = null;
            return;
        }
        this.endDate = endDateTime.toLocalDate();
        this.endTime = endDateTime.toLocalTime();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPriceFrom() {
        return priceFrom;
    }

    public void setPriceFrom(BigDecimal priceFrom) {
        this.priceFrom = priceFrom;
    }

    public BigDecimal getPriceTo() {
        return priceTo;
    }

    public void setPriceTo(BigDecimal priceTo) {
        this.priceTo = priceTo;
    }

    public String getPriceNote() {
        return priceNote;
    }

    public void setPriceNote(String priceNote) {
        this.priceNote = priceNote;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean recurring) {
        isRecurring = recurring;
    }

    public String getRecurringPattern() {
        return recurringPattern;
    }

    public void setRecurringPattern(String recurringPattern) {
        this.recurringPattern = recurringPattern;
    }

    public String getScrapedFrom() {
        return scrapedFrom;
    }

    public void setScrapedFrom(String scrapedFrom) {
        this.scrapedFrom = scrapedFrom;
    }

    public String getParserVersion() {
        return parserVersion;
    }

    public void setParserVersion(String parserVersion) {
        this.parserVersion = parserVersion;
    }

    public String getDeduplicationHash() {
        return deduplicationHash;
    }

    public void setDeduplicationHash(String deduplicationHash) {
        this.deduplicationHash = deduplicationHash;
    }

    public String getRawSourceHtml() {
        return rawSourceHtml;
    }

    public void setRawSourceHtml(String rawSourceHtml) {
        this.rawSourceHtml = rawSourceHtml;
    }

    public java.util.Set<String> getTags() {
        return tags;
    }

    public void setTags(java.util.Set<String> tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
