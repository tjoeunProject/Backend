package com.example.project.route.controller;

import com.example.project.route.dto.RouteCreateRequestDto;
import com.example.project.route.dto.RouteDetailResponseDto;
import com.example.project.route.dto.RouteListItemDto;
import com.example.project.route.service.RouteService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RouteController
 * ---------------------------------------
 * 일정(Route) 관련 REST API 제공
 *
 * 기능:
 *  - 일정 생성 POST /api/route
 *  - 상세 조회 GET /api/route/{routeId}
 *  - 특정 회원의 일정 목록 조회 GET /api/route/member/{memberId}
 *  - 일정 수정 PUT /api/route/{routeId}
 *  - 일정 삭제 DELETE /api/route/{routeId}
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/route")
public class RouteController {

    private final RouteService routeService;

    /** 일정 생성 */
    @PostMapping
    public Long createRoute(@RequestBody RouteCreateRequestDto dto) {
        return routeService.createRoute(dto);
    }

    /** 일정 상세 조회 */
    @GetMapping("/{routeId}")
    public RouteDetailResponseDto getRouteDetail(
            @PathVariable("routeId") Long routeId
    ) {
        return routeService.getRouteDetail(routeId);
    }

    /** 특정 회원의 일정 목록 조회 */
    @GetMapping("/member/{memberId}")
    public List<RouteListItemDto> getRoutesByMember(
            @PathVariable("memberId") Long memberId
    ) {
        return routeService.getRoutesByMember(memberId);
    }

    /** 일정 수정 */
    @PutMapping("/{routeId}")
    public void updateRoute(
            @PathVariable("routeId") Long routeId,
            @RequestBody RouteCreateRequestDto dto
    ) {
        routeService.updateRoute(routeId, dto);
    }

    /** 일정 삭제 */
    @DeleteMapping("/{routeId}")
    public void deleteRoute(
            @PathVariable("routeId") Long routeId
    ) {
        routeService.deleteRoute(routeId);
    }
}
