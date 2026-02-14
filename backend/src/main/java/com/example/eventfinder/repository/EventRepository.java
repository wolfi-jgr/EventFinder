package com.example.eventfinder.repository;

import com.example.eventfinder.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    
    // Find events by category
    List<Event> findByCategory(String category);
    
    // Find upcoming events (events that haven't ended yet)
    @Query("SELECT e FROM Event e WHERE e.endDateTime >= :now OR (e.endDateTime IS NULL AND e.startDateTime >= :now) ORDER BY e.startDateTime")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now);
    
    // Find events within a date range
    @Query("SELECT e FROM Event e WHERE e.startDateTime BETWEEN :startDate AND :endDate ORDER BY e.startDateTime")
    List<Event> findEventsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find events by location (simple string matching)
    List<Event> findByLocationContainingIgnoreCase(String location);
    
    // Find events near coordinates (basic bounding box query)
    @Query("SELECT e FROM Event e WHERE e.latitude BETWEEN :minLat AND :maxLat AND e.longitude BETWEEN :minLon AND :maxLon ORDER BY e.startDateTime")
    List<Event> findEventsNearCoordinates(
        @Param("minLat") Double minLat,
        @Param("maxLat") Double maxLat,
        @Param("minLon") Double minLon,
        @Param("maxLon") Double maxLon
    );
}
