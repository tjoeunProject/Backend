package com.example.project.place.service;

import com.example.project.place.domain.Place;
import com.example.project.place.dto.PlaceRequestDto;
import com.example.project.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    public Place savePlace(PlaceRequestDto dto) {

        // 이미 저장된 구글 장소일 경우 업데이트 대신 return (중복 저장 방지)
        if (placeRepository.existsByGooglePlaceId(dto.getGooglePlaceId())) {
            return placeRepository.findByGooglePlaceId(dto.getGooglePlaceId());
        }

        Place place = new Place();

        place.setGooglePlaceId(dto.getGooglePlaceId());
        place.setName(dto.getName());
        place.setFormattedAddress(dto.getFormattedAddress());

        place.setLat(dto.getLat());
        place.setLng(dto.getLng());

        place.setRating(dto.getRating());
        place.setUserRatingsTotal(dto.getUserRatingsTotal());

        place.setTypes(dto.getTypes());

        place.setPhotoReferences(dto.getPhotoReferences());
        place.setHtmlAttributions(dto.getHtmlAttributions());
        place.setPhotoWidth(dto.getPhotoWidth());
        place.setPhotoHeight(dto.getPhotoHeight());

        return placeRepository.save(place);
    }
}
