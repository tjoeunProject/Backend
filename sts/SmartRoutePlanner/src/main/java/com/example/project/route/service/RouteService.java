package com.example.project.route.service; // 👉 수정 필요

import com.example.project.place.domain.Place;
import com.example.project.place.repository.PlaceRepository; // 👉 패키지 맞게 수정
import com.example.project.route.domain.Route;
import com.example.project.route.domain.RoutePlace;
import com.example.project.route.dto.*;
import com.example.project.route.repository.RoutePlaceRepository;
import com.example.project.route.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 여행 일정(Route) 관련 핵심 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RouteService {

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;
    private final PlaceRepository placeRepository; // Place 엔티티 조회용

    /**
     * 일정 생성
     * - Route 저장
     * - RoutePlace 리스트 저장
     * - totalDays는 startDate~endDate 기준으로 자동 계산
     */
    public Long createRoute(RouteCreateRequestDto dto) {

        Route route = new Route();
        route.setMemberId(dto.getMemberId());
        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());

        // totalDays 계산: end - start + 1
        int totalDays = (int) ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        route.setTotalDays(totalDays);

        // 우선 Route만 저장
        Route savedRoute = routeRepository.save(route);

        // 하루별 장소(RoutePlace) 생성 및 저장
        if (dto.getPlaces() != null) {
            for (RoutePlaceRequestDto placeDto : dto.getPlaces()) {

                Place place = placeRepository.findById(placeDto.getPlaceId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 placeId: " + placeDto.getPlaceId()));

                RoutePlace routePlace = new RoutePlace();
                routePlace.setRoute(savedRoute);
                routePlace.setPlace(place);
                routePlace.setDayIndex(placeDto.getDayIndex());
                routePlace.setOrderIndex(placeDto.getOrderIndex());

                routePlaceRepository.save(routePlace);
            }
        }

        return savedRoute.getId();
    }

    /**
     * 일정 상세 조회
     * - Route 기본 정보
     * - RoutePlace들을 dayIndex 기준으로 묶어서 리턴
     */
    @Transactional(readOnly = true)
    public RouteDetailResponseDto getRouteDetail(Long routeId) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 routeId: " + routeId));

        List<RoutePlace> routePlaces =
                routePlaceRepository.findByRouteIdOrderByDayIndexAscOrderIndexAsc(routeId);

        // dayIndex 기준으로 그룹핑
        Map<Integer, List<RoutePlace>> groupedByDay = routePlaces.stream()
                .collect(Collectors.groupingBy(RoutePlace::getDayIndex, LinkedHashMap::new, Collectors.toList()));

        List<DayItineraryDto> dayItineraries = new ArrayList<>();

        for (Map.Entry<Integer, List<RoutePlace>> entry : groupedByDay.entrySet()) {
            int dayIndex = entry.getKey();
            List<RoutePlace> dayPlaces = entry.getValue();

            List<PlaceSummaryDto> placeSummaries = dayPlaces.stream()
                    .map(rp -> new PlaceSummaryDto(
                            rp.getPlace().getId(),
                            rp.getPlace().getName(),
                            rp.getOrderIndex()
                    ))
                    .toList();

            dayItineraries.add(new DayItineraryDto(dayIndex, placeSummaries));
        }

        return new RouteDetailResponseDto(
                route.getId(),
                route.getMemberId(),
                route.getTitle(),
                route.getStartDate(),
                route.getEndDate(),
                route.getTotalDays(),
                dayItineraries
        );
    }

    /**
     * 특정 회원이 만든 일정 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RouteListItemDto> getRoutesByMember(Long memberId) {

        List<Route> routes = routeRepository.findByMemberId(memberId);

        return routes.stream()
                .map(route -> new RouteListItemDto(
                        route.getId(),
                        route.getTitle(),
                        route.getStartDate(),
                        route.getEndDate(),
                        route.getTotalDays()
                ))
                .toList();
    }

    /**
     * 일정 수정
     * - 기본 정보 수정
     * - 기존 RoutePlace 전부 삭제 후, 새로 저장하는 방식 (심플 버전)
     */
    public void updateRoute(Long routeId, RouteCreateRequestDto dto) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 routeId: " + routeId));

        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());
        int totalDays = (int) ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        route.setTotalDays(totalDays);

        // 기존 RoutePlace 모두 삭제
        routePlaceRepository.deleteByRouteId(routeId);

        // 새 RoutePlace 재생성
        if (dto.getPlaces() != null) {
            for (RoutePlaceRequestDto placeDto : dto.getPlaces()) {

                Place place = placeRepository.findById(placeDto.getPlaceId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 placeId: " + placeDto.getPlaceId()));

                RoutePlace routePlace = new RoutePlace();
                routePlace.setRoute(route);
                routePlace.setPlace(place);
                routePlace.setDayIndex(placeDto.getDayIndex());
                routePlace.setOrderIndex(placeDto.getOrderIndex());

                routePlaceRepository.save(routePlace);
            }
        }

        // Route는 영속 상태라 save() 다시 안 해도 변경 내용 flush 됨
    }

    /**
     * 일정 삭제
     * - RoutePlace 먼저 삭제 후 Route 삭제
     */
    public void deleteRoute(Long routeId) {

        if (!routeRepository.existsById(routeId)) {
            throw new IllegalArgumentException("존재하지 않는 routeId: " + routeId);
        }

        routePlaceRepository.deleteByRouteId(routeId);
        routeRepository.deleteById(routeId);
    }
}
