package main.java.com.example.eventfinder.service;

import com.example.eventfinder.model.Place;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScrapeService {
    public List<Place> fetchPlacesStub(double latitude, double longitude) {
        List<Place> items = new ArrayList<>();

        Place example = new Place();
        example.setName("Example Cafe");
        example.setCategory("Food");
        example.setLatitude(latitude);
        example.setLongitude(longitude);
        example.setSourceUrl("https://example.com");
        example.setLastUpdated(Instant.now());

        items.add(example);
        return items;
    }
}
