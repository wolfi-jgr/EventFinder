package com.example.eventfinder.controller;

import com.example.eventfinder.model.Place;
import com.example.eventfinder.service.PlaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/places")
public class PlaceController {
    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public List<Place> list(@RequestParam double lat, @RequestParam double lon) {
        return placeService.getPlaces(lat, lon);
    }

    @GetMapping("/cached")
    public List<Place> cached() {
        return placeService.getCachedPlaces();
    }
}
