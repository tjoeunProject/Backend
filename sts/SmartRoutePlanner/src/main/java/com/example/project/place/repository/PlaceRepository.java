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
 */

package com.example.project.place.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.project.place.domain.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    boolean existsByGooglePlaceId(String googlePlaceId);

    Place findByGooglePlaceId(String googlePlaceId);

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
}
