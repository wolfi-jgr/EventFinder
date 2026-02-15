package com.example.eventfinder.service;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {
    private final EventRepository eventRepository;
    private final ScrapeService scrapeService;

    public EventService(EventRepository eventRepository, ScrapeService scrapeService) {
        this.eventRepository = eventRepository;
        this.scrapeService = scrapeService;
    }

    /**
     * Get all events from the database
     */
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    /**
     * Get event by ID
     */
    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    /**
     * Get upcoming events (events that haven't ended yet)
     */
    public List<Event> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDateTime.now());
    }

    /**
     * Get events by category
     */
    public List<Event> getEventsByCategory(String category) {
        return eventRepository.findByCategory(category);
    }

    /**
     * Get events within a date range
     */
    public List<Event> getEventsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return eventRepository.findEventsByDateRange(startDate, endDate);
    }

    /**
     * Get events by location (string matching)
     */
    public List<Event> getEventsByLocation(String location) {
        return eventRepository.findByLocationContainingIgnoreCase(location);
    }

    /**
     * Get events near coordinates within a radius (approximate using bounding box)
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radiusKm Radius in kilometers
     */
    public List<Event> getEventsNearCoordinates(double latitude, double longitude, double radiusKm) {
        // Approximate degree offset for the given radius
        // 1 degree latitude ≈ 111 km
        // 1 degree longitude varies by latitude, but we'll use a simple approximation
        double latOffset = radiusKm / 111.0;
        double lonOffset = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));

        double minLat = latitude - latOffset;
        double maxLat = latitude + latOffset;
        double minLon = longitude - lonOffset;
        double maxLon = longitude + lonOffset;

        return eventRepository.findEventsNearCoordinates(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Fetch and save events from external sources (to be implemented with scraper)
     */
    public List<Event> fetchAndSaveEvents(double latitude, double longitude) {
        // Fetch events from scraper
        List<Event> fetchedEvents = scrapeService.fetchEventsStub(latitude, longitude);
        
        // Save and return the fetched events
        return saveAllEvents(fetchedEvents);
    }

    /**
     * Create or update an event
     */
    public Event saveEvent(Event event) {
        event.setLastUpdated(Instant.now());
        return eventRepository.save(event);
    }

    /**
     * Delete an event by ID
     */
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    /**
     * Delete all events from the database
     */
    public long deleteAllEvents() {
        long count = eventRepository.count();
        eventRepository.deleteAll();
        return count;
    }

    /**
     * Bulk save events (useful for scrapers)
     */
    public List<Event> saveAllEvents(List<Event> events) {
        for (Event event : events) {
            event.setLastUpdated(Instant.now());
        }
        return eventRepository.saveAll(events);
    }
    
    /**
     * Save events with duplicate checking
     * Only saves events that don't already exist in the database
     * An event is considered duplicate if it has the same title, start date/time, and location
     * 
     * @param events List of events to save
     * @return SaveResult with counts of new, duplicate, and updated events
     */
    public SaveResult saveEventsWithDeduplication(List<Event> events) {
        int newCount = 0;
        int duplicateCount = 0;
        int updatedCount = 0;
        List<Event> savedEvents = new ArrayList<>();
        
        for (Event event : events) {
            // Skip events without required fields
            if (event.getTitle() == null || event.getStartDateTime() == null) {
                continue;
            }
            
            // Provide default location if missing
            if (event.getLocation() == null || event.getLocation().trim().isEmpty()) {
                event.setLocation("Unknown");
            }
            
            // Check for duplicates
            List<Event> existingEvents = eventRepository.findDuplicateEvents(
                event.getTitle(),
                event.getStartDateTime(),
                event.getLocation()
            );
            
            if (existingEvents.isEmpty()) {
                // New event - save it
                event.setLastUpdated(Instant.now());
                Event savedEvent = eventRepository.save(event);
                savedEvents.add(savedEvent);
                newCount++;
            } else {
                // Duplicate found - optionally update if new data is different
                Event existingEvent = existingEvents.get(0);
                boolean needsUpdate = false;
                
                // Update fields that might have changed
                if (event.getDescription() != null && !event.getDescription().equals(existingEvent.getDescription())) {
                    existingEvent.setDescription(event.getDescription());
                    needsUpdate = true;
                }
                
                if (event.getEndDateTime() != null && !event.getEndDateTime().equals(existingEvent.getEndDateTime())) {
                    existingEvent.setEndDateTime(event.getEndDateTime());
                    needsUpdate = true;
                }
                
                if (event.getImageUrl() != null && !event.getImageUrl().equals(existingEvent.getImageUrl())) {
                    existingEvent.setImageUrl(event.getImageUrl());
                    needsUpdate = true;
                }
                
                if (event.getPrice() != null && !event.getPrice().equals(existingEvent.getPrice())) {
                    existingEvent.setPrice(event.getPrice());
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    existingEvent.setLastUpdated(Instant.now());
                    eventRepository.save(existingEvent);
                    savedEvents.add(existingEvent);
                    updatedCount++;
                } else {
                    duplicateCount++;
                }
            }
        }
        
        return new SaveResult(newCount, duplicateCount, updatedCount, savedEvents);
    }
    
    /**
     * Result of saving events with deduplication
     */
    public static class SaveResult {
        private final int newEvents;
        private final int duplicateEvents;
        private final int updatedEvents;
        private final List<Event> savedEvents;
        
        public SaveResult(int newEvents, int duplicateEvents, int updatedEvents, List<Event> savedEvents) {
            this.newEvents = newEvents;
            this.duplicateEvents = duplicateEvents;
            this.updatedEvents = updatedEvents;
            this.savedEvents = savedEvents;
        }
        
        public int getNewEvents() { return newEvents; }
        public int getDuplicateEvents() { return duplicateEvents; }
        public int getUpdatedEvents() { return updatedEvents; }
        public List<Event> getSavedEvents() { return savedEvents; }
        public int getTotalProcessed() { return newEvents + duplicateEvents + updatedEvents; }
    }
}
