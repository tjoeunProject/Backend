package com.example.project.route.service;

import com.example.project.route.domain.Route;
import com.example.project.route.domain.RoutePlace;
import com.example.project.route.dto.*;
import com.example.project.route.dto.RouteCreateRequestDto.SimplePlaceDto;
import com.example.project.route.repository.RoutePlaceRepository;
import com.example.project.route.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 여행 일정(Route) 비즈니스 로직
 * - 특징 1: PlaceRepository 의존성 제거 (파이썬에서 받은 데이터 신뢰)
 * - 특징 2: RoutePlace에 객체 대신 ID와 이름(placeName) 직접 저장
 * - 특징 3: 프론트에서 [[1일차], [2일차]] 형태의 이중 리스트로 데이터 수신
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RouteService {

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;

    /**
     * 일정 생성
     */
    public Long createRoute(RouteCreateRequestDto dto) {
        log.info("일정 생성 요청: 제목={}, 회원ID={}", dto.getTitle(), dto.getMemberId());

        // 1. Route(부모) 저장
        Route route = new Route();
        route.setMemberId(dto.getMemberId());
        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());

        // 날짜 차이 계산 (시작일~종료일)
        int totalDays = (int) ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        route.setTotalDays(totalDays);

        Route savedRoute = routeRepository.save(route);
        log.info("Route 저장 완료. ID: {}", savedRoute.getId());

        // 2. RoutePlace(자식) 저장 - 이중 리스트 파싱
        saveRoutePlaces(savedRoute, dto.getPlaces());

        return savedRoute.getId();
    }

    /**
     * 일정 상세 조회
     */
    @Transactional(readOnly = true)
    public RouteDetailResponseDto getRouteDetail(Long routeId) {
        
        // 1. Route 조회
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 routeId: " + routeId));

        // 2. RoutePlace 조회 (정렬: 일자 -> 순서)
        List<RoutePlace> routePlaces = 
                routePlaceRepository.findByRouteIdOrderByDayIndexAscOrderIndexAsc(routeId);

        // 3. 일자별(dayIndex) 그룹핑
        Map<Integer, List<RoutePlace>> groupedByDay = routePlaces.stream()
                .collect(Collectors.groupingBy(RoutePlace::getDayIndex, LinkedHashMap::new, Collectors.toList()));

        // 4. 응답 DTO 변환
        List<DayItineraryDto> dayItineraries = new ArrayList<>();

        for (Map.Entry<Integer, List<RoutePlace>> entry : groupedByDay.entrySet()) {
            int dayIndex = entry.getKey();
            List<RoutePlace> dayPlaces = entry.getValue();

            // 장소 정보 변환 (저장된 placeName 사용)
            List<PlaceSummaryDto> placeSummaries = dayPlaces.stream()
                    .map(rp -> new PlaceSummaryDto(
                            rp.getPlaceId(),
                            rp.getPlaceName(), // ★ DB에 저장해둔 이름 꺼내기
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
     * 내 일정 목록 조회
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
     * - 전략: 기존 장소 전부 삭제(Delete) -> 새 장소 전체 저장(Insert)
     */
    public void updateRoute(Long routeId, RouteCreateRequestDto dto) {
        log.info("일정 수정 요청. ID: {}", routeId);

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 routeId: " + routeId));

        // 1. 기본 정보 수정 (Dirty Checking)
        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());
        
        int totalDays = (int) ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        route.setTotalDays(totalDays);

        // 2. 기존 장소 삭제
        routePlaceRepository.deleteByRouteId(routeId);
        log.info("기존 장소 데이터 삭제 완료");

        // 3. 새 장소 저장
        saveRoutePlaces(route, dto.getPlaces());
        log.info("새 장소 데이터 저장 완료");
    }

    /**
     * 일정 삭제
     */
    public void deleteRoute(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new IllegalArgumentException("존재하지 않는 routeId: " + routeId);
        }
        // 자식 먼저 삭제 -> 부모 삭제
        routePlaceRepository.deleteByRouteId(routeId);
        routeRepository.deleteById(routeId);
        log.info("일정 삭제 완료. ID: {}", routeId);
    }

    
    // =================================================================
    // [보조 메서드] 장소 리스트 저장 로직 (생성, 수정에서 공통 사용)
    // =================================================================
    private void saveRoutePlaces(Route route, List<List<SimplePlaceDto>> placeGrid) {
        if (placeGrid == null || placeGrid.isEmpty()) return;

        for (int i = 0; i < placeGrid.size(); i++) {
            List<SimplePlaceDto> dailyPlaces = placeGrid.get(i);
            int dayIndex = i + 1; // 1일차부터 시작

            for (int j = 0; j < dailyPlaces.size(); j++) {
                SimplePlaceDto placeDto = dailyPlaces.get(j);
                int orderIndex = j + 1; // 1번부터 시작

                RoutePlace routePlace = new RoutePlace();
                routePlace.setRoute(route);
                
                // ★ ID와 이름 둘 다 저장
                routePlace.setPlaceId(placeDto.getPlaceId());
                routePlace.setPlaceName(placeDto.getPlaceName()); 

                routePlace.setDayIndex(dayIndex);
                routePlace.setOrderIndex(orderIndex);

                routePlaceRepository.save(routePlace);
            }
        }
    }
}