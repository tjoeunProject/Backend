package com.example.project.route.service;

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
import com.example.project.route.dto.DayItineraryDto;
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
     * 일정 상세 조회
     */
    public RouteDetailResponseDto getRouteDetail(Long routeId) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Route not found id=" + routeId));

        List<RoutePlace> places =
                routePlaceRepository.findByRouteIdOrderByDayIndexAscOrderIndexAsc(routeId);

        Map<Integer, List<PlaceSummaryDto>> grouped =
                places.stream().collect(Collectors.groupingBy(
                        RoutePlace::getDayIndex,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                rp -> new PlaceSummaryDto(
                                        rp.getPlace().getId(),
                                        rp.getPlaceName(),
                                        rp.getOrderIndex()),
                                Collectors.toList()
                        )
                ));

        List<DayItineraryDto> days = grouped.entrySet().stream()
                .map(e -> new DayItineraryDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new RouteDetailResponseDto(
                route.getId(),
                route.getUser().getId().longValue(),
                route.getTitle(),
                route.getStartDate(),
                route.getEndDate(),
                days
        );
    }

    /**
     * 특정 회원의 일정 목록 조회
     */
    public List<RouteListItemDto> getRoutesByMember(Long memberId) {

        List<Route> routes =
                routeRepository.findAllByUser_Id(memberId.intValue());

        // List<Route> -> List<RouteListItemDto> 변환
        return routes.stream()
                .map(RouteListItemDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 일정 수정
     */
    @Transactional
    public void updateRoute(Long routeId, RouteCreateRequestDto dto) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Route not found id=" + routeId));

        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());
        route.setTotalDays(dto.getPlaces().size());

        routePlaceRepository.deleteByRouteId(routeId);

        int dayIndex = 1;
        for (List<RouteCreateRequestDto.SimplePlaceDto> day : dto.getPlaces()) {

            int orderIndex = 1;
            for (RouteCreateRequestDto.SimplePlaceDto sp : day) {

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
