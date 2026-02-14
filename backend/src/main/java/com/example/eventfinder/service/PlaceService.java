package main.java.com.example.eventfinder.service;

import com.example.eventfinder.model.Place;
import com.example.eventfinder.repository.PlaceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class PlaceService {
    private final PlaceRepository placeRepository;
    private final ScrapeService scrapeService;

    public PlaceService(PlaceRepository placeRepository, ScrapeService scrapeService) {
        this.placeRepository = placeRepository;
        this.scrapeService = scrapeService;
    }

    public List<Place> getPlaces(double latitude, double longitude) {
        List<Place> fetched = scrapeService.fetchPlacesStub(latitude, longitude);

        // Basic cache write for now; later, replace with real merge/dedup logic.
        for (Place place : fetched) {
            place.setLastUpdated(Instant.now());
        }

        return placeRepository.saveAll(fetched);
    }

    public List<Place> getCachedPlaces() {
        return placeRepository.findAll();
    }
}
