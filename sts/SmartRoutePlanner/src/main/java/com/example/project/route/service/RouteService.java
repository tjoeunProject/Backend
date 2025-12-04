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

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final RoutePlaceRepository routePlaceRepository;
    private final TravelUserRepository travelUserRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public Long createRoute(RouteCreateRequestDto dto) {

        TravelUser user = travelUserRepository.findById(dto.getMemberId())
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
                        .orElseThrow(() -> new IllegalArgumentException("Place not found: " + sp.getPlaceId()));

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

    public RouteDetailResponseDto getRouteDetail(Long routeId) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found id=" + routeId));

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

    public List<RouteListItemDto> getRoutesByMember(Long memberId) {
        return routeRepository.findAllByUser_Id(memberId).stream()
                .map(RouteListItemDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateRoute(Long routeId, RouteCreateRequestDto dto) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found id=" + routeId));

        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());
        route.setTotalDays(dto.getPlaces().size());

        routePlaceRepository.deleteByRouteId(routeId);

        int dayIndex = 1;
        for (List<RouteCreateRequestDto.SimplePlaceDto> daily : dto.getPlaces()) {
            int orderIndex = 1;

            for (RouteCreateRequestDto.SimplePlaceDto sp : daily) {

                Place place = placeRepository.findById(sp.getPlaceId())
                        .orElseThrow(() -> new IllegalArgumentException("Place not found: " + sp.getPlaceId()));

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

    @Transactional
    public void deleteRoute(Long routeId) {
        routeRepository.deleteById(routeId);
        routePlaceRepository.deleteByRouteId(routeId);
    }
}
