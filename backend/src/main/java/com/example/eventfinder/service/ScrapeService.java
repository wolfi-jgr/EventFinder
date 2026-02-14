package com.example.eventfinder.service;

import com.example.eventfinder.model.Event;
import com.example.eventfinder.model.Place;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScrapeService {
    public List<Place> fetchPlacesStub(double latitude, double longitude) {
        List<Place> items = new ArrayList<>();

        Place place1 = new Place();
        place1.setName("Café Central");
        place1.setCategory("Cafe");
        place1.setLatitude(48.2108);
        place1.setLongitude(16.3654);
        place1.setSourceUrl("https://www.cafecentral.wien");

        Place place2 = new Place();
        place2.setName("Naschmarkt");
        place2.setCategory("Market");
        place2.setLatitude(48.1985);
        place2.setLongitude(16.3633);
        place2.setSourceUrl("https://www.naschmarkt-vienna.com");

        Place place3 = new Place();
        place3.setName("Prater");
        place3.setCategory("Park");
        place3.setLatitude(48.2165);
        place3.setLongitude(16.3958);
        place3.setSourceUrl("https://www.prater.at");

        items.add(place1);
        items.add(place2);
        items.add(place3);
        return items;
    }

    public List<Event> fetchEventsStub(double latitude, double longitude) {
        List<Event> events = new ArrayList<>();

        Event event1 = new Event();
        event1.setTitle("Vienna Opera Ball");
        event1.setDescription("The most prestigious ball in Vienna, held at the Vienna State Opera. A night of classical music, dancing, and elegance.");
        event1.setStartDateTime(LocalDateTime.of(2026, 2, 27, 22, 0));
        event1.setEndDateTime(LocalDateTime.of(2026, 2, 28, 5, 0));
        event1.setLocation("Vienna State Opera");
        event1.setLatitude(48.2031);
        event1.setLongitude(16.3691);
        event1.setCategory("Culture");
        event1.setSourceUrl("https://www.wiener-staatsoper.at");
        event1.setImageUrl("https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=800");
        event1.setOrganizer("Vienna State Opera");
        event1.setPrice("From 290 EUR");
        event1.setStatus("SCHEDULED");

        Event event2 = new Event();
        event2.setTitle("Vienna Jazz Festival");
        event2.setDescription("International jazz artists perform at various venues across Vienna. Three nights of world-class jazz music.");
        event2.setStartDateTime(LocalDateTime.of(2026, 3, 15, 20, 0));
        event2.setEndDateTime(LocalDateTime.of(2026, 3, 17, 23, 30));
        event2.setLocation("Porgy & Bess");
        event2.setLatitude(48.2082);
        event2.setLongitude(16.3738);
        event2.setCategory("Music");
        event2.setSourceUrl("https://www.porgy.at");
        event2.setImageUrl("https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=800");
        event2.setOrganizer("Porgy & Bess");
        event2.setPrice("25-45 EUR");
        event2.setStatus("SCHEDULED");

        Event event3 = new Event();
        event3.setTitle("Prater Spring Festival");
        event3.setDescription("Traditional spring festival at the famous Prater amusement park with rides, food stalls, and live entertainment.");
        event3.setStartDateTime(LocalDateTime.of(2026, 4, 1, 10, 0));
        event3.setEndDateTime(LocalDateTime.of(2026, 4, 30, 23, 0));
        event3.setLocation("Wiener Prater");
        event3.setLatitude(48.2165);
        event3.setLongitude(16.3958);
        event3.setCategory("Festival");
        event3.setSourceUrl("https://www.prater.at");
        event3.setImageUrl("https://images.unsplash.com/photo-1513151233558-d860c5398176?w=800");
        event3.setOrganizer("Wiener Prater");
        event3.setPrice("Free entry");
        event3.setStatus("SCHEDULED");

        events.add(event1);
        events.add(event2);
        events.add(event3);
        return events;
    }
}
