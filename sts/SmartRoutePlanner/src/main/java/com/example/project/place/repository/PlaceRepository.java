package com.example.project.place.repository;

import com.example.project.place.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    boolean existsByGooglePlaceId(String googlePlaceId);

    Place findByGooglePlaceId(String googlePlaceId);
}
