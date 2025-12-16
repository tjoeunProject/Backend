package com.example.project.place.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.project.global.exception.PlaceApiException;
import com.example.project.place.dto.PlaceRequestDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * GooglePlacesClient (New Places API)
 * -------------------------------------------------------
 * Google New Places API(v1) 기반 Place Details 조회
 *
 * Endpoint:
 *   GET https://places.googleapis.com/v1/places/{placeId}
 *
 * 특징:
 *   - 프론트 Google Maps JS placeId와 100% 호환
 *   - fields는 Header(X-Goog-FieldMask)로 지정
 */
@Service
public class GooglePlacesClient {

    @Value("${google.places.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * placeId 기반 Google Place Details 조회 (New API)
     */
    public PlaceRequestDto fetchPlaceDetails(String placeId) {

        String url = "https://places.googleapis.com/v1/places/" + placeId + "?languageCode=ko";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set(
                "X-Goog-FieldMask",
                String.join(",",
                        "id",
                        "displayName",
                        "formattedAddress",
                        "location",
                        "rating",
                        "userRatingCount",
                        "types",
                        "photos"
                )
        );

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PlaceDetailsResponse> responseEntity =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        PlaceDetailsResponse.class
                );

        PlaceDetailsResponse response = responseEntity.getBody();

        if (response == null) {
            throw new PlaceApiException("Google New Places API 응답이 null입니다.");
        }

        // New API는 status 필드 없음 → id 존재 여부로 판단
        if (response.getId() == null) {
            throw new PlaceApiException(
                    "Google Place Details NOT_FOUND, placeId=" + placeId
            );
        }

        // ---------------- DTO 변환 ----------------
        PlaceRequestDto dto = new PlaceRequestDto();

        dto.setGooglePlaceId(response.getId());
        dto.setName(response.getDisplayName() != null
                ? response.getDisplayName().getText()
                : null);
        dto.setFormattedAddress(response.getFormattedAddress());

        if (response.getLocation() != null) {
            dto.setLat(response.getLocation().getLatitude());
            dto.setLng(response.getLocation().getLongitude());
        }

        dto.setRating(response.getRating());
        dto.setUserRatingsTotal(response.getUserRatingCount());
        dto.setTypes(response.getTypes() != null
                ? response.getTypes()
                : Collections.emptyList());

        // 사진 (첫 장만 사용)
        if (response.getPhotos() != null && !response.getPhotos().isEmpty()) {
            Photo photo = response.getPhotos().get(0);
            dto.setPhotoReferences(
                    List.of(photo.getName())
            );
            dto.setHtmlAttributions(Collections.emptyList());
            dto.setPhotoWidth(0);
            dto.setPhotoHeight(0);
        } else {
            dto.setPhotoReferences(Collections.emptyList());
            dto.setHtmlAttributions(Collections.emptyList());
            dto.setPhotoWidth(0);
            dto.setPhotoHeight(0);
        }

        return dto;
    }

    // ---------------- New Places API 응답 매핑 ----------------

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaceDetailsResponse {

        private String id;

        private DisplayName displayName;

        private String formattedAddress;

        private Location location;

        private double rating;

        private int userRatingCount;

        private List<String> types;

        private List<Photo> photos;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DisplayName {
        private String text;
        private String languageCode;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double latitude;
        private double longitude;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Photo {
        // New API 사진은 name으로 식별
        private String name;
    }
}
