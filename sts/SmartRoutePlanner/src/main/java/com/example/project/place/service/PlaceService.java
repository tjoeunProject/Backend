/**
 * PlaceService
 * ----------------------------
 * Place 저장 / 조회 / 검색 / 필터링 등
 * 장소 관련 비즈니스 로직을 담당한다.
 *
 * 기능:
 *  - Place 저장 (Google JSON 기반)
 *  - 상세 조회
 *  - 키워드 검색
 *  - 타입 기반 검색
 *  - 주변 장소(nearby) 검색
 *  - Google Places API 자동 저장 기능
 *  - 타입별 인기 장소 조회
 */

package com.example.project.place.service;

import com.example.project.place.domain.Place;
import com.example.project.place.dto.PlaceRequestDto;
import com.example.project.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final GooglePlacesClient googlePlacesClient;

    /**
     * Place 저장 (Google JSON 기반)
     * 이미 저장된 googlePlaceId가 있으면 기존 데이터 반환한다.
     */
    public Place savePlace(PlaceRequestDto dto) {

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

    /**
     * Place 상세 조회
     */
    public Place getPlaceDetail(Long id) {
        return placeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 장소를 찾을 수 없습니다. id=" + id));
    }

    /**
     * 키워드 기반 검색
     * name, formattedAddress, types 문자열 기반 LIKE 검색
     */
    public List<Place> searchPlace(String keyword) {
        return placeRepository.searchByKeyword(keyword);
    }

    /**
     * 타입 기반 검색
     * restaurant, cafe, park 등 구글 Place type 포함 여부로 조회
     */
    public List<Place> getPlacesByType(String type) {
        return placeRepository.findByTypesContains(type);
    }

    /**
     * 주변 장소(nearby) 검색
     * 기준 좌표(lat, lng)와 반경(radiusMeters) 내의 장소 조회
     * 거리 계산은 Haversine 공식을 사용
     */
    public List<Place> getNearbyPlaces(double lat, double lng, double radiusMeters, String type) {
        List<Place> allPlaces = placeRepository.findAll();

        return allPlaces.stream()
                .filter(place -> {
                    if (type == null || type.isBlank()) {
                        return true;
                    }
                    return place.getTypes() != null && place.getTypes().contains(type);
                })
                .filter(place -> {
                    double distance = calculateDistanceMeters(lat, lng, place.getLat(), place.getLng());
                    return distance <= radiusMeters;
                })
                .sorted(Comparator.comparingDouble(
                        place -> calculateDistanceMeters(lat, lng, place.getLat(), place.getLng()))
                )
                .collect(Collectors.toList());
    }

    /**
     * 두 좌표 간 거리를 계산하는 Haversine 공식 (단위: 미터)
     */
    private double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * Google Places API를 호출하여 placeId로 Place 저장
     * 저장 시 savePlace(dto)를 재사용해 중복 체크 처리
     */
    public Place savePlaceFromGoogle(String placeId) {
        PlaceRequestDto dto = googlePlacesClient.fetchPlaceDetails(placeId);
        return savePlace(dto);
    }

    /**
     * 타입 기반 인기 장소 조회
     * rating과 userRatingsTotal(리뷰 수) 기준 상위 limit개 조회
     */
    public List<Place> getPopularPlaces(String type, int limit) {
        List<Place> places = placeRepository
                .findByTypesContainsOrderByRatingDescUserRatingsTotalDesc(type);

        return places.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
