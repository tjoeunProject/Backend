/**
 * PlaceService
 * -----------------------------------------------------------------
 * 역할(중요):
 *   - 우리 서비스 내부의 "Place" 관련 비즈니스 로직을 모두 처리한다.
 *     즉, DB 저장 / 조회 / 검색 / 거리 계산 / 인기 장소 정렬 등
 *     '도메인 로직'을 담당한다.
 *
 * GooglePlacesClient와의 관계:
 *   - GooglePlacesClient는 "Google API 호출"만 담당하는 외부 통신 모듈이고,
 *   - PlaceService는 그 데이터(dto)를 기반으로
 *     DB에 저장하고 검색/필터링 등 내부 로직을 처리하는 계층이다.
 *
 * 왜 분리되어야 하는가?
 *   1) 단일 책임 원칙(SRP) 유지
 *       - 외부 API 변경이 PlaceService 로직에 영향을 주면 안 됨.
 *
 *   2) 테스트 용이성 증가
 *       - 외부 API는 Mock 처리하고, PlaceService는 도메인 로직만 테스트 가능.
 *
 *   3) 유지보수 및 확장성
 *       - Google → Kakao API로 바꿔도 PlaceService는 그대로 유지됨.
 *
 *   4) 도메인 독립성 확보
 *       - PlaceService는 “우리 어플의 장소 관리 규칙”만 알고 있어야 함.
 *
 * 주의:
 *   - PlaceService는 외부 API 실패를 그대로 전파하지 않고
 *     도메인 예외(PlaceApiException)로 변환한다.
 *
 * 사용처:
 *   - Route 생성 시 DB에 place 저장
 *   - 프론트가 장소 검색하면 DB 내에서 필터링/검색 반환
 *   - 인기 장소 추천 API
 *   - Nearby 검색 등 거리 계산 기반 기능
 */

package com.example.project.place.service;

import com.example.project.global.exception.PlaceApiException;
import com.example.project.place.domain.Place;
import com.example.project.place.dto.PlaceRequestDto;
import com.example.project.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final GooglePlacesClient googlePlacesClient;

    /**
     * Google API에서 가져온 PlaceRequestDto를 DB Place 엔티티로 저장한다.
     * 이미 googlePlaceId가 존재하면 기존 데이터 재활용 → 중복 저장 방지.
     *
     * 내부 로직:
     *   - DTO → 엔티티 변환
     *   - DB 저장
     *
     * 사용처:
     *   - 경로 저장 시 placeId만 넘어와도 자동으로 상세 정보 저장 가능
     */
    public Place savePlace(PlaceRequestDto dto) {

        return placeRepository.findOptionalByGooglePlaceId(dto.getGooglePlaceId())
                .orElseGet(() -> {
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
                });
    }

    /**
     * ID 기반 상세 조회
     * DB 직접 조회 (외부 API 호출 없음)
     */
    @Transactional(readOnly = true)
    public Place getPlaceDetail(Long id) {
        return placeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("해당 장소를 찾을 수 없습니다. id=" + id));
    }

    /**
     * 키워드 기반 검색
     * name, formattedAddress, types 문자열 기반 LIKE 검색
     */
    @Transactional(readOnly = true)
    public List<Place> searchPlace(String keyword) {
        return placeRepository.searchByKeyword(keyword);
    }

    /**
     * 특정 타입(restaurant, cafe 등)에 해당하는 장소 조회
     * Google type 기준
     */
    @Transactional(readOnly = true)
    public List<Place> getPlacesByType(String type) {
        return placeRepository.findByTypesContains(type);
    }

    /**
     * 현재 좌표(lat/lng)에서 반경 내 장소 검색
     * 거리 계산 → Haversine 공식 사용
     *
     * 성능 개선:
     *   - Bounding Box로 DB 1차 필터
     *   - 실제 거리 계산은 Java에서 처리
     *
     * 사용처:
     *   - "현재 위치 주변 맛집 추천" 같은 기능
     */
    @Transactional(readOnly = true)
    public List<Place> getNearbyPlaces(double lat, double lng, double radiusMeters, String type) {

        double deltaLat = radiusMeters / 111000.0;
        double deltaLng = radiusMeters / (111000.0 * Math.cos(Math.toRadians(lat)));

        List<Place> candidates =
                placeRepository.findWithinBoundingBox(
                        lat - deltaLat,
                        lat + deltaLat,
                        lng - deltaLng,
                        lng + deltaLng
                );

        return candidates.stream()
                .filter(place -> {
                    if (type == null || type.isBlank()) {
                        return true;
                    }
                    return place.getTypes() != null && place.getTypes().contains(type);
                })
                .filter(place -> {
                    double distance =
                            calculateDistanceMeters(lat, lng, place.getLat(), place.getLng());
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
    private double calculateDistanceMeters(
            double lat1, double lng1, double lat2, double lng2) {

        final int EARTH_RADIUS = 6371000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * Google Places API를 호출하여 placeId로 Place 저장
     * 저장 시 savePlace(dto)를 재사용해 중복 체크 처리
     *
     * 외부 API 실패 시 PlaceApiException으로 변환
     */
    public Place savePlaceFromGoogle(String placeId) {

        return placeRepository.findOptionalByGooglePlaceId(placeId)
                .orElseGet(() -> {

                    PlaceRequestDto dto =
                            googlePlacesClient.fetchPlaceDetails(placeId);

                    
                    if (dto == null) {
                        throw new PlaceApiException(
                                "Google Place Details 응답 null placeId=" + placeId
                        );
                    }

                    if (dto.getLat() == 0 || dto.getLng() == 0) {
                        throw new PlaceApiException(
                                "Google Place Details 좌표 정보 없음 placeId=" + placeId
                        );
                    }

                    return savePlace(dto);
                });
    }

    /**
     * 타입 기반 인기 장소 조회
     * rating과 userRatingsTotal(리뷰 수) 기준 상위 limit개 조회
     */
    @Transactional(readOnly = true)
    public List<Place> getPopularPlaces(String type, int limit) {

        List<Place> places =
                placeRepository.findByTypesContainsOrderByRatingDescUserRatingsTotalDesc(type);

        return places.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
