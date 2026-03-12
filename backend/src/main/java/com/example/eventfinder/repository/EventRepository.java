package com.example.eventfinder.repository;

import com.example.eventfinder.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    
    // Find events by category
    List<Event> findByCategory(String category);
    
    // Find upcoming events (events that haven't ended yet)
    @Query("SELECT e FROM Event e WHERE " +
            "(e.endDate IS NOT NULL AND (e.endDate > :currentDate OR (e.endDate = :currentDate AND (e.endTime IS NULL OR e.endTime >= :currentTime)))) " +
            "OR " +
            "(e.endDate IS NULL AND (e.startDate > :currentDate OR (e.startDate = :currentDate AND (e.startTime IS NULL OR e.startTime >= :currentTime)))) " +
            "ORDER BY e.startDate, e.startTime")
    List<Event> findUpcomingEvents(@Param("currentDate") LocalDate currentDate, @Param("currentTime") LocalTime currentTime);
    
    // Find events within a date range
    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :startDate AND :endDate ORDER BY e.startDate, e.startTime")
    List<Event> findEventsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Find events by location (simple string matching)
    List<Event> findByLocationContainingIgnoreCase(String location);
    
    // Find events near coordinates (basic bounding box query)
    @Query("SELECT e FROM Event e WHERE e.latitude BETWEEN :minLat AND :maxLat AND e.longitude BETWEEN :minLon AND :maxLon ORDER BY e.startDate, e.startTime")
    List<Event> findEventsNearCoordinates(
        @Param("minLat") Double minLat,
        @Param("maxLat") Double maxLat,
        @Param("minLon") Double minLon,
        @Param("maxLon") Double maxLon
    );
    
    // Find duplicate event by title, start date, and location (for deduplication during scraping)
    @Query("SELECT e FROM Event e WHERE LOWER(e.title) = LOWER(:title) " +
            "AND e.startDate = :startDate " +
            "AND ((e.startTime IS NULL AND :startTime IS NULL) OR e.startTime = :startTime) " +
            "AND LOWER(e.location) = LOWER(:location)")
    List<Event> findDuplicateEvents(
        @Param("title") String title,
        @Param("startDate") LocalDate startDate,
        @Param("startTime") LocalTime startTime,
        @Param("location") String location
    );
    
    // Check if event exists by deduplication hash
    boolean existsByDeduplicationHash(String deduplicationHash);
    
    // Count events from a specific scraper source
    long countByScrapedFrom(String scrapedFrom);

    // Delete all events from a specific scraper source
    long deleteByScrapedFrom(String scrapedFrom);
}
