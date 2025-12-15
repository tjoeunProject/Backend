/**
 * PlaceRepository
 * ----------------------------
 * Place 엔티티에 대한 DB 접근을 담당한다.
 *
 * 기능:
 *  - googlePlaceId 기준 존재 여부 확인 및 조회
 *  - 키워드 검색 (name, formattedAddress, types)
 *  - 타입 포함 검색
 *  - 타입 기반 인기 장소 조회 (rating, 리뷰 수 정렬)
 *  - Nearby 검색을 위한 Bounding Box 1차 필터
 */

package com.example.project.place.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.project.place.domain.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * googlePlaceId 존재 여부 확인
     */
    boolean existsByGooglePlaceId(String googlePlaceId);

    /**
     * googlePlaceId로 Place 조회 (기존 방식)
     */
    Place findByGooglePlaceId(String googlePlaceId);

    /**
     * googlePlaceId로 Place 조회 (Optional 방식)
     * PlaceService에서 중복 조회 제거를 위해 사용
     */
    Optional<Place> findOptionalByGooglePlaceId(String googlePlaceId);

    /**
     * 키워드 검색
     * name, formattedAddress, types 컬렉션 중 하나라도 keyword를 포함하는 Place를 조회한다.
     */
    @Query("SELECT DISTINCT p FROM Place p LEFT JOIN p.types t " +
           "WHERE p.name LIKE %:keyword% " +
           "OR p.formattedAddress LIKE %:keyword% " +
           "OR t LIKE %:keyword%")
    List<Place> searchByKeyword(String keyword);

    /**
     * 타입 포함 검색
     * types 컬렉션에 해당 type 문자열을 포함하는 Place를 조회한다.
     */
    List<Place> findByTypesContains(String type);

    /**
     * 타입 기반 인기 장소 조회
     * types에 type을 포함하고, rating과 userRatingsTotal 기준으로 정렬한다.
     */
    List<Place> findByTypesContainsOrderByRatingDescUserRatingsTotalDesc(String type);

    /**
     * Nearby 검색 성능 개선용 Bounding Box 1차 필터
     * PlaceService.getNearbyPlaces()에서 사용
     */
    @Query("""
        select p
        from Place p
        where p.lat between :minLat and :maxLat
          and p.lng between :minLng and :maxLng
    """)
    List<Place> findWithinBoundingBox(
            double minLat,
            double maxLat,
            double minLng,
            double maxLng
    );
}
