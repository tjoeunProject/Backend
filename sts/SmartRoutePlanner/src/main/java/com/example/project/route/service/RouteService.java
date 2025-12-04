package com.example.project.route.service;

import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.place.domain.Place;
import com.example.project.place.repository.PlaceRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RouteService
 * ---------------------------------------
 * 일정(Route)과 관련된 비즈니스 로직을 처리하는 서비스.
 *
 * 기능:
 *  - 일정 생성
 *  - 일정 상세 조회
 *  - 일정 목록 조회
 *  - 일정 수정
 *  - 일정 삭제
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;
    private final TravelUserRepository travelUserRepository;
    private final PlaceRepository placeRepository;

    /**
     * 일정 생성
     * 1) Route 저장
     * 2) RoutePlace들을 dayIndex, orderIndex에 맞게 저장
     */
    @Transactional
    public Long createRoute(RouteCreateRequestDto dto) {

        // TravelUser PK는 Integer → DTO는 Long → 변환 필요
        TravelUser user = travelUserRepository.findById(dto.getMemberId().intValue())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. id=" + dto.getMemberId()));

        Route route = new Route();
        route.setUser(user);
        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());
        route.setTotalDays(dto.getPlaces().size());

        Route saved = routeRepository.save(route);

        int dayIndex = 1;
        for (List<RouteCreateRequestDto.SimplePlaceDto> dailyPlaces : dto.getPlaces()) {
            int orderIndex = 1;

            for (RouteCreateRequestDto.SimplePlaceDto sp : dailyPlaces) {

                Place place = placeRepository.findById(sp.getPlaceId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Place not found: " + sp.getPlaceId()));

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
     * RoutePlace를 dayIndex → orderIndex 순으로 정렬하여 DTO 형태로 조립한다.
     */
    public RouteDetailResponseDto getRouteDetail(Long routeId) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Route not found id=" + routeId));

        List<RoutePlace> places =
                routePlaceRepository.findByRouteIdOrderByDayIndexAscOrderIndexAsc(routeId);

        Map<Integer, List<PlaceSummaryDto>> grouped = places.stream()
                .collect(Collectors.groupingBy(
                        RoutePlace::getDayIndex,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                rp -> new PlaceSummaryDto(
                                        rp.getPlace().getId(),
                                        rp.getPlaceName(),
                                        rp.getOrderIndex()
                                ),
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

        return routes.stream()
                .map(RouteListItemDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 일정 수정
     * 기존 RoutePlace를 전부 삭제 후 새로 저장하는 방식 사용
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

        // 기존 모든 RoutePlace 제거
        routePlaceRepository.deleteByRouteId(routeId);

        int dayIndex = 1;
        for (List<RouteCreateRequestDto.SimplePlaceDto> daily : dto.getPlaces()) {
            int orderIndex = 1;

            for (RouteCreateRequestDto.SimplePlaceDto sp : daily) {

                Place place = placeRepository.findById(sp.getPlaceId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Place not found: " + sp.getPlaceId()));

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
        // 순서 주의! RoutePlace 먼저 삭제
        routePlaceRepository.deleteByRouteId(routeId);
        routeRepository.deleteById(routeId);
    }
}
