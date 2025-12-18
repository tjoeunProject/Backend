package com.example.project.route.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.place.domain.Place;
import com.example.project.place.service.PlaceService;
import com.example.project.route.domain.Route;
import com.example.project.route.domain.RoutePlace;
import com.example.project.route.dto.PlaceSummaryDto;
import com.example.project.route.dto.RouteCreateRequestDto;
import com.example.project.route.dto.RouteDetailResponseDto;
import com.example.project.route.dto.RouteListItemDto;
import com.example.project.route.repository.RoutePlaceRepository;
import com.example.project.route.repository.RouteRepository;

import lombok.RequiredArgsConstructor;

/**
 * RouteService
 * 일정(Route) 관련 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;
    private final TravelUserRepository travelUserRepository;
    private final PlaceService placeService;

    /**
     * 일정 생성
     */
    @Transactional
    public Long createRoute(RouteCreateRequestDto dto) {

        TravelUser user = travelUserRepository.findById(dto.getMemberId().intValue())
                .orElseThrow(() ->
                        new IllegalArgumentException("회원 없음 id=" + dto.getMemberId()));

        Route route = new Route();
        route.setUser(user);
        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());
        route.setTotalDays(dto.getPlaces().size());

        Route saved = routeRepository.save(route);

        int dayIndex = 1;
     // day 변수: 해당 날짜에 방문할 장소들의 리스트 (List<SimplePlaceDto>)
        for (List<RouteCreateRequestDto.SimplePlaceDto> day : dto.getPlaces()) {

            int orderIndex = 1;
         // [안쪽 for문]: 해당 날짜의 장소(Place)들을 순회합니다.
            for (RouteCreateRequestDto.SimplePlaceDto sp : day) {

            	Place place = placeService.savePlaceFromGoogle(sp.getPlaceId());
            	
                RoutePlace rp = new RoutePlace();
                
                rp.setRoute(saved);
                rp.setPlace(place);
                rp.setPlaceName(place.getName());
                rp.setDayIndex(dayIndex);
                rp.setOrderIndex(orderIndex);

                routePlaceRepository.save(rp);
                orderIndex++;
            }
            dayIndex++;
        }

        return saved.getId();
    }

    /**
     * 일정 상세 조회 (최종 수정본) // 수정 3번함... 
     */
    public RouteDetailResponseDto getRouteDetail(Long routeId) {

        // 1. Route(일정) 본체 조회
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found id=" + routeId));

        // 2. RoutePlace(연결정보) 조회 
        // ★ 핵심 1: DB에서 가져올 때 이미 dayIndex, orderIndex 순으로 정렬되어 옴
        List<RoutePlace> routePlaces =
                routePlaceRepository.findByRouteIdOrderByDayIndexAscOrderIndexAsc(routeId);

        // 3. 그룹화 및 Place 데이터 매핑
        Map<Integer, List<PlaceSummaryDto>> grouped = routePlaces.stream()
                .collect(Collectors.groupingBy(
                        RoutePlace::getDayIndex,
                        LinkedHashMap::new,
                        Collectors.mapping(rp -> {
                            // ★ 핵심 2: RoutePlace와 매칭된 Place 객체 데이터를 꺼냄 (JPA가 ID 매칭 처리)
                            Place p = rp.getPlace(); 
                            
                            // Place 정보를 담을 DTO 생성
                            PlaceSummaryDto dto = new PlaceSummaryDto();
                         // 1. Types
                            dto.setTypes(new ArrayList<>(p.getTypes())); 

                            // 2. PhotoReferences
                            if (p.getPhotoReferences() != null) {
                                dto.setPhotoReferences(new ArrayList<>(p.getPhotoReferences()));
                            } else {
                                dto.setPhotoReferences(new ArrayList<>());
                            }
                            
                            // 3. HtmlAttributions
                            if (p.getHtmlAttributions() != null) {
                                dto.setHtmlAttributions(new ArrayList<>(p.getHtmlAttributions()));
                            } else {
                                 dto.setHtmlAttributions(new ArrayList<>());
                            }
                            // [Place 객체 데이터 복사] 
                            // PlaceResponseDto에 있는 모든 필드를 그대로 옮겨 담습니다.
                            dto.setId(p.getId());
                            dto.setGooglePlaceId(p.getGooglePlaceId());
                            dto.setName(p.getName());
                            dto.setFormattedAddress(p.getFormattedAddress());
                            dto.setLat(p.getLat());
                            dto.setLng(p.getLng());
                            dto.setRating(p.getRating());
                            dto.setUserRatingsTotal(p.getUserRatingsTotal());
                            dto.setOrderIndex(rp.getOrderIndex());

                            return dto;
                        }, Collectors.toList())
                ));

        // 4. Map -> 2차원 List 변환 (빈 날짜 처리 포함)
        List<List<PlaceSummaryDto>> places2d = new ArrayList<>();
        
        // 1일차부터 총 일수(totalDays)까지 루프를 돌며 리스트 생성
        for (int i = 1; i <= route.getTotalDays(); i++) {
            List<PlaceSummaryDto> dayPlaces = grouped.getOrDefault(i, Collections.emptyList());
            places2d.add(dayPlaces);
        }

        // 5. 최종 반환
        return new RouteDetailResponseDto(
                route.getId(),
                route.getUser().getId().longValue(),
                route.getTitle(),
                route.getStartDate(),
                route.getEndDate(),
                places2d // ★ Place 객체 데이터가 담긴 2차원 리스트
        );
    }
    
    
    

    /**
     * 특정 회원의 일정 목록 조회
     */
    public List<RouteListItemDto> getRoutesByMember(Long memberId) {

        List<Route> routes =
                routeRepository.findAllByUser_Id(memberId.intValue());

     // Stream map을 사용하여 각 Route마다 첫 번째 장소를 찾아서 DTO 생성
        return routes.stream()
                .map(route -> {
                    // 1. 해당 루트의 첫 번째 장소(RoutePlace) 조회
                    RoutePlace firstRp = routePlaceRepository
                            .findFirstByRouteIdOrderByDayIndexAscOrderIndexAsc(route.getId())
                            .orElse(null);

                    // 2. RoutePlace에서 Place(장소 원본) 꺼내기 (없으면 null)
                    Place firstPlace = (firstRp != null) ? firstRp.getPlace() : null;

                    // 3. DTO 생성 (Route + Place 정보)
                    return new RouteListItemDto(route, firstPlace);
                })
                .collect(Collectors.toList());
    }

   
    /**
     * 일정 수정 (최종 수정본)
     */
    @Transactional
    public void updateRoute(Long routeId, RouteCreateRequestDto dto) {

        // 1. 기존 일정 조회 및 기본 정보 업데이트
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found id=" + routeId));

        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());
        route.setTotalDays(dto.getPlaces().size());

        // 2. 기존 장소들 모두 삭제 (Delete All)
        routePlaceRepository.deleteByRouteId(routeId);

        // 3. 새 장소 목록 다시 등록 (Insert All) - createRoute와 동일한 로직 적용
        int dayIndex = 1;
        for (List<RouteCreateRequestDto.SimplePlaceDto> day : dto.getPlaces()) {
            int orderIndex = 1;
            for (RouteCreateRequestDto.SimplePlaceDto sp : day) {

                // [수정 포인트] findById가 아니라 savePlaceFromGoogle 사용!
                // sp.getPlaceId()는 이제 String(Google ID)입니다.
                Place place = placeService.savePlaceFromGoogle(sp.getPlaceId());

                RoutePlace rp = new RoutePlace();
                rp.setRoute(route);
                rp.setPlace(place);
                rp.setPlaceName(place.getName());
                rp.setDayIndex(dayIndex);
                rp.setOrderIndex(orderIndex);

                routePlaceRepository.save(rp);
                orderIndex++;
            }
            dayIndex++;
        }
    }

    /**
     * 일정 삭제
     */
    @Transactional
    public void deleteRoute(Long routeId) {
        routePlaceRepository.deleteByRouteId(routeId);
        routeRepository.deleteById(routeId);
    }
}
