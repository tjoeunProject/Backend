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
/**
 * GooglePlacesClient
 * -------------------------------------------------------
 * 역할:
 *   - Google Places **Details API(placeId 기반 상세정보)** 를 호출하여
 *     Raw JSON → 내부 자바 객체(Result 등) → PlaceRequestDto 형태로 변환한다.
 *
 * 사용처(중요):
 *   - 프론트엔드에서 장소를 선택하거나 저장할 때, placeId만 넘어오면
 *     백엔드에서 GooglePlacesClient.fetchPlaceDetails(placeId)를 호출하여
 *     실제 장소 정보(이름, 주소, 좌표, 사진, 타입 등)를 가져온다.
 *   - Route 생성, 장소 등록, 리뷰 기능 등에서 Google API 데이터를 DB에 넣기 전
 *     DTO 형태로 가공하는 용도로 사용된다.
 *
 * 작동 방식 요약:
 *   1) RestTemplate을 사용하여 Google Place Details API 호출
 *   2) Google JSON → 내부 클래스(PlaceDetailsResponse/Result...)로 역직렬화
 *   3) 필요한 필드를 추출해서 PlaceRequestDto로 변환
 *   4) Controller/Service에서 DB 저장 또는 프론트 전달에 활용
 *
 * 필요 설정:
 *   application.properties:
 *       google.places.api-key=YOUR_GOOGLE_API_KEY
 */


@Service
public class GooglePlacesClient {

    @Value("${google.places.api-key}")
    private String apiKey;

    // Google REST API 호출 담당
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * fetchPlaceDetails()
     * -------------------------------------------------------
     * placeId를 받아 Google Place Details API를 호출한 뒤,
     * PlaceRequestDto 형태로 변환하여 반환한다.
     *
     * 사용 위치:
     *   - PlaceController, RouteService 등에서
     *     "placeId만 넘어온 상황"에서 상세 정보가 필요할 때 호출된다.
     *
     * 예시:
     *   POST /api/route
     *   {
     *       "placeId": "ChIJd_Y0eVIvkFQRl0g2LhCA0SU"
     *   }
     *   → placeId 기준 Google 상세 정보 조회 → DB 저장용 DTO 생성
     */
    public PlaceRequestDto fetchPlaceDetails(String placeId) {

        // Google Place Details API 요청 URL 구성
        String url = "https://maps.googleapis.com/maps/api/place/details/json"
                + "?place_id=" + placeId
                + "&key=" + apiKey
                + "&language=ko";

        // Google JSON 응답을 PlaceDetailsResponse로 역직렬화
        PlaceDetailsResponse response =
                restTemplate.getForObject(url, PlaceDetailsResponse.class);

        if (response == null || response.getResult() == null) {
            throw new RuntimeException("Google Place Details 응답이 비어 있습니다. placeId=" + placeId);
        }

        Result result = response.getResult();

        // Google raw data → PlaceRequestDto 형태로 변환
        PlaceRequestDto dto = new PlaceRequestDto();
        dto.setGooglePlaceId(result.getPlaceId());
        dto.setName(result.getName());
        dto.setFormattedAddress(result.getFormattedAddress());

        // 좌표 매핑
        if (result.getGeometry() != null && result.getGeometry().getLocation() != null) {
            dto.setLat(result.getGeometry().getLocation().getLat());
            dto.setLng(result.getGeometry().getLocation().getLng());
        }

        // 평점 정보
        dto.setRating(result.getRating());
        dto.setUserRatingsTotal(result.getUserRatingsTotal());
        dto.setTypes(result.getTypes() != null ? result.getTypes() : Collections.emptyList());

        // 사진 정보 세팅 (첫 번째 사진만 사용)
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

    // ---------------- Google API JSON 매핑용 내부 클래스들 ----------------

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaceDetailsResponse {
        // Google 응답: { "result": { ... } }
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

        private List<String> types;    // 음식점, 관광지 등 카테고리

        private List<Photo> photos;    // 사진 정보 배열
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        private Location location; // lat/lng 구조
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
        private String photoReference; // 사진 요청 시 필요한 key

        private int width;
        private int height;

        @JsonProperty("html_attributions")
        private List<String> htmlAttributions; // 저작권 출처
    }
}
