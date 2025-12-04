/**
 * GooglePlacesClient
 * ----------------------------
 * Google Places Details API를 호출하여 placeId 기반 장소 상세 정보를 가져오고,
 * PlaceRequestDto로 변환하는 역할을 담당한다.
 *
 * 설정:
 *  - application.properties 또는 yml에 다음 속성이 필요하다.
 *    google.places.api-key=YOUR_GOOGLE_API_KEY
 */

package com.example.project.place.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.project.place.dto.PlaceRequestDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Service
public class GooglePlacesClient {

    @Value("${google.places.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Google Place Details API를 호출하여 PlaceRequestDto로 변환한다.
     */
    public PlaceRequestDto fetchPlaceDetails(String placeId) {

        String url = "https://maps.googleapis.com/maps/api/place/details/json"
                + "?place_id=" + placeId
                + "&key=" + apiKey
                + "&language=ko";

        PlaceDetailsResponse response =
                restTemplate.getForObject(url, PlaceDetailsResponse.class);

        if (response == null || response.getResult() == null) {
            throw new RuntimeException("Google Place Details 응답이 비어 있습니다. placeId=" + placeId);
        }

        Result result = response.getResult();

        PlaceRequestDto dto = new PlaceRequestDto();
        dto.setGooglePlaceId(result.getPlaceId());
        dto.setName(result.getName());
        dto.setFormattedAddress(result.getFormattedAddress());

        if (result.getGeometry() != null && result.getGeometry().getLocation() != null) {
            dto.setLat(result.getGeometry().getLocation().getLat());
            dto.setLng(result.getGeometry().getLocation().getLng());
        }

        dto.setRating(result.getRating());
        dto.setUserRatingsTotal(result.getUserRatingsTotal());
        dto.setTypes(result.getTypes() != null ? result.getTypes() : Collections.emptyList());

        if (result.getPhotos() != null && !result.getPhotos().isEmpty()) {
            Photo photo = result.getPhotos().get(0);
            dto.setPhotoReferences(List.of(photo.getPhotoReference()));
            dto.setHtmlAttributions(photo.getHtmlAttributions());
            dto.setPhotoWidth(photo.getWidth());
            dto.setPhotoHeight(photo.getHeight());
        } else {
            dto.setPhotoReferences(Collections.emptyList());
            dto.setHtmlAttributions(Collections.emptyList());
            dto.setPhotoWidth(0);
            dto.setPhotoHeight(0);
        }

        return dto;
    }

    // 아래는 Google Places API 응답 매핑용 내부 클래스들

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaceDetailsResponse {
        private Result result;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {

        @JsonProperty("place_id")
        private String placeId;

        private String name;

        @JsonProperty("formatted_address")
        private String formattedAddress;

        private Geometry geometry;

        private double rating;

        @JsonProperty("user_ratings_total")
        private int userRatingsTotal;

        private List<String> types;

        private List<Photo> photos;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        private Location location;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double lat;
        private double lng;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Photo {

        @JsonProperty("photo_reference")
        private String photoReference;

        private int width;
        private int height;

        @JsonProperty("html_attributions")
        private List<String> htmlAttributions;
    }
}
