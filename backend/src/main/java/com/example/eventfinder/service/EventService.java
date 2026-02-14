package com.example.eventfinder.service;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
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
     * Bulk save events (useful for scrapers)
     */
    public List<Event> saveAllEvents(List<Event> events) {
        for (Event event : events) {
            event.setLastUpdated(Instant.now());
        }
        return eventRepository.saveAll(events);
    }
}
