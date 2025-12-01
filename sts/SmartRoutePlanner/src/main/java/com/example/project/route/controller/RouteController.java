package com.example.project.route.controller; // 👉 수정 필요

import com.example.project.route.dto.RouteCreateRequestDto;
import com.example.project.route.dto.RouteDetailResponseDto;
import com.example.project.route.dto.RouteListItemDto;
import com.example.project.route.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * /api/route 하위의 REST API를 제공하는 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/route")
public class RouteController {

    private final RouteService routeService;

    /**
     * 일정 생성
     * POST /api/route
     */
    @PostMapping
    public Long createRoute(@RequestBody RouteCreateRequestDto dto) {
        return routeService.createRoute(dto);
    }

    /**
     * 일정 상세 조회
     * GET /api/route/{routeId}
     */
    @GetMapping("/{routeId}")
    public RouteDetailResponseDto getRouteDetail(@PathVariable Long routeId) {
        return routeService.getRouteDetail(routeId);
    }

    /**
     * 특정 회원의 일정 목록 조회
     * GET /api/route/member/{memberId}
     */
    @GetMapping("/member/{memberId}")
    public List<RouteListItemDto> getRoutesByMember(@PathVariable Long memberId) {
        return routeService.getRoutesByMember(memberId);
    }

    /**
     * 일정 수정
     * PUT /api/route/{routeId}
     */
    @PutMapping("/{routeId}")
    public void updateRoute(@PathVariable Long routeId,
                            @RequestBody RouteCreateRequestDto dto) {
        routeService.updateRoute(routeId, dto);
    }

    /**
     * 일정 삭제
     * DELETE /api/route/{routeId}
     */
    @DeleteMapping("/{routeId}")
    public void deleteRoute(@PathVariable Long routeId) {
        routeService.deleteRoute(routeId);
    }
}
