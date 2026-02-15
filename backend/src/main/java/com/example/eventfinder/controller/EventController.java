package com.example.eventfinder.controller;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.service.EventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * GET /api/events - Get all events
     */
    @GetMapping
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
    }

    /**
     * GET /api/events/{id} - Get event by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return eventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/events/upcoming - Get upcoming events
     */
    @GetMapping("/upcoming")
    public List<Event> getUpcomingEvents() {
        return eventService.getUpcomingEvents();
    }

    /**
     * GET /api/events/category/{category} - Get events by category
     */
    @GetMapping("/category/{category}")
    public List<Event> getEventsByCategory(@PathVariable String category) {
        return eventService.getEventsByCategory(category);
    }

    /**
     * GET /api/events/search - Search events by date range
     * Example: /api/events/search?startDate=2026-02-14T00:00:00&endDate=2026-02-28T23:59:59
     */
    @GetMapping("/search")
    public List<Event> searchEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return eventService.getEventsByDateRange(startDate, endDate);
    }

    /**
     * GET /api/events/location - Search events by location
     * Example: /api/events/location?q=Berlin
     */
    @GetMapping("/location")
    public List<Event> getEventsByLocation(@RequestParam String q) {
        return eventService.getEventsByLocation(q);
    }

    /**
     * GET /api/events/nearby - Get events near coordinates
     * Example: /api/events/nearby?lat=52.5200&lon=13.4050&radius=10
     */
    @GetMapping("/nearby")
    public List<Event> getEventsNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10.0") double radius) {
        return eventService.getEventsNearCoordinates(lat, lon, radius);
    }

    /**
     * POST /api/events - Create a new event
     */
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        Event savedEvent = eventService.saveEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEvent);
    }

    /**
     * PUT /api/events/{id} - Update an existing event
     */
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event event) {
        return eventService.getEventById(id)
                .map(existingEvent -> {
                    event.setId(id);
                    Event updatedEvent = eventService.saveEvent(event);
                    return ResponseEntity.ok(updatedEvent);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/events/{id} - Delete an event
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        return eventService.getEventById(id)
                .map(event -> {
                    eventService.deleteEvent(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/events - Delete all events (reset database)
     */
    @DeleteMapping
    public ResponseEntity<?> deleteAllEvents() {
        long deletedCount = eventService.deleteAllEvents();
        return ResponseEntity.ok()
                .body(java.util.Map.of(
                    "message", "All events deleted successfully",
                    "deletedCount", deletedCount
                ));
    }

    /**
     * POST /api/events/fetch - Fetch and save events from external sources
     * Example: /api/events/fetch?lat=52.5200&lon=13.4050
     */
    @PostMapping("/fetch")
    public List<Event> fetchEvents(@RequestParam double lat, @RequestParam double lon) {
        return eventService.fetchAndSaveEvents(lat, lon);
    }
}
