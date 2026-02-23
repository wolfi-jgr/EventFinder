package com.example.eventfinder.service;

import com.example.eventfinder.model.Place;
import com.example.eventfinder.repository.PlaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlaceService {
    private final PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    /**
     * Get all cached places
     */
    public List<Place> getCachedPlaces() {
        return placeRepository.findAll();
    }

    /**
     * Get places near given coordinates (within approximate 50km radius)
     */
    public List<Place> getPlaces(double lat, double lon) {
        // Default radius of 50km
        double radiusKm = 50.0;
        return getPlacesNearCoordinates(lat, lon, radiusKm);
    }

    /**
     * Get places near coordinates within a specified radius
     */
    public List<Place> getPlacesNearCoordinates(double latitude, double longitude, double radiusKm) {
        double latOffset = radiusKm / 111.0;
        double lonOffset = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));

        double minLat = latitude - latOffset;
        double maxLat = latitude + latOffset;
        double minLon = longitude - lonOffset;
        double maxLon = longitude + lonOffset;

        List<Place> places = placeRepository.findAll();
        return places.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .filter(p -> p.getLatitude() >= minLat && p.getLatitude() <= maxLat &&
                           p.getLongitude() >= minLon && p.getLongitude() <= maxLon)
                .collect(Collectors.toList());
    }
}

