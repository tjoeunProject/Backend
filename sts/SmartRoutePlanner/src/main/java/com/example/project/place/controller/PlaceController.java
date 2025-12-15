/**
 * PlaceController
 * ----------------------------
 * '장소(Place)'에 대한 HTTP API를 제공한다.
 *
 * 기능:
 *  - Place 저장 (구글 장소 JSON 또는 Google API 기반)
 *  - Place 상세 조회
 *  - 키워드 기반 검색
 *  - 타입 기반 검색
 *  - 주변 장소(nearby) 검색
 *  - Google Places API 자동 저장 호출
 *  - 타입별 인기 장소 조회
 */

package com.example.project.place.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.place.domain.Place;
import com.example.project.place.dto.PlaceRequestDto;
import com.example.project.place.dto.PlaceResponseDto;
import com.example.project.place.service.PlaceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place")
public class PlaceController {

    private final PlaceService placeService;

    /**
     * Place 저장
     * 프론트에서 Google Places API 응답(JSON)을 가공한 PlaceRequestDto를 보내는 경우 사용.
     */
    @PostMapping
    public PlaceResponseDto savePlace(@RequestBody PlaceRequestDto dto) {
        Place place = placeService.savePlace(dto);
        return toDto(place);
    }

    /**
     * Place 상세 조회
     */
    @GetMapping("/{id}")
    public PlaceResponseDto getPlaceDetail(@PathVariable Long id) {
        Place place = placeService.getPlaceDetail(id);
        return toDto(place);
    }

    /**
     * 키워드 검색
     * name, formattedAddress, types 문자열 기준으로 검색한다.
     */
    @GetMapping("/search")
    public List<PlaceResponseDto> searchPlace(@RequestParam String keyword) {
        List<Place> places = placeService.searchPlace(keyword);
        return toDtoList(places);
    }

    /**
     * 타입 기반 검색
     * restaurant, cafe, park 등 구글 place type 기준.
     */
    @GetMapping("/type/{type}")
    public List<PlaceResponseDto> getPlacesByType(@PathVariable String type) {
        List<Place> places = placeService.getPlacesByType(type);
        return toDtoList(places);
    }

    /**
     * 주변 장소(nearby) 검색
     * 기준 좌표(lat, lng)와 반경(radiusMeters) 내의 장소를 조회한다.
     * type이 전달되면 해당 타입을 포함한 장소만 필터링한다.
     */
    @GetMapping("/nearby")
    public List<PlaceResponseDto> getNearbyPlaces(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") double radiusMeters,
            @RequestParam(required = false) String type
    ) {
        List<Place> places = placeService.getNearbyPlaces(lat, lng, radiusMeters, type);
        return toDtoList(places);
    }

    /**
     * Google Places API를 직접 호출하여 placeId 기반으로 Place를 저장한다.
     * 프론트에서는 placeId만 넘기면 되고,
     * 서버에서 Google API를 호출해 Place 정보를 저장한다.
     */
    @GetMapping("/google")
    public PlaceResponseDto savePlaceFromGoogle(@RequestParam String placeId, @RequestParam Long routeId) {
        Place place = placeService.savePlaceFromGoogle(placeId, routeId);
        return toDto(place);
    }

    /**
     * 타입별 인기 장소 조회
     * rating, userRatingsTotal 기준으로 상위 limit개 조회.
     */
    @GetMapping("/popular")
    public List<PlaceResponseDto> getPopularPlaces(
            @RequestParam String type,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Place> places = placeService.getPopularPlaces(type, limit);
        return toDtoList(places);
    }

    // 엔티티 → DTO 변환 메서드들

    private PlaceResponseDto toDto(Place place) {
        PlaceResponseDto dto = new PlaceResponseDto();
        dto.setId(place.getId());
        dto.setGooglePlaceId(place.getGooglePlaceId());
        dto.setName(place.getName());
        dto.setFormattedAddress(place.getFormattedAddress());
        dto.setLat(place.getLat());
        dto.setLng(place.getLng());
        dto.setRating(place.getRating());
        dto.setUserRatingsTotal(place.getUserRatingsTotal());
        dto.setTypes(place.getTypes());
        dto.setPhotoReferences(place.getPhotoReferences());
        dto.setHtmlAttributions(place.getHtmlAttributions());
        dto.setPhotoWidth(place.getPhotoWidth());
        dto.setPhotoHeight(place.getPhotoHeight());
        return dto;
    }

    private List<PlaceResponseDto> toDtoList(List<Place> places) {
        return places.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
