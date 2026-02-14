package com.example.eventfinder.repository;

import com.example.eventfinder.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
}
