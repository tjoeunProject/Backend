package com.example.project.route.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.route.domain.Route;
import com.example.project.route.dto.RouteCreateRequestDto;
import com.example.project.route.dto.RouteDetailResponseDto;
import com.example.project.route.dto.RouteListItemDto;
import com.example.project.route.repository.RouteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 (성능 최적화)
public class RouteService {

    private final RouteRepository routeRepository;
    private final TravelUserRepository travelUserRepository;

    /**
     * 일정 생성
     */
    @Transactional // 쓰기 작업이므로 readOnly = false
    public Long createRoute(RouteCreateRequestDto dto) {
        // 1. 회원(TravelUser) 조회
        // DTO로 들어온 memberId를 이용해 실제 DB에서 유저 객체를 가져옵니다.
        TravelUser user = travelUserRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. id=" + dto.getMemberId()));

        // 2. 일정(Route) 엔티티 생성
        Route route = new Route();
        route.setUser(user); // ★ 핵심: 조회한 유저 객체를 넣어줍니다. (관계 설정)
        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());
        
        // 3. 저장
        Route savedRoute = routeRepository.save(route);
        return savedRoute.getId();
    }

    /**
     * 일정 상세 조회
     */
    public RouteDetailResponseDto getRouteDetail(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정이 없습니다. id=" + routeId));

        // 엔티티 -> DTO 변환 (생성자나 빌더 패턴 사용 권장)
        // 여기서는 예시로 Setter 혹은 생성자를 가정하고 작성합니다.
        return new RouteDetailResponseDto(route); 
    }

    /**
     * 특정 회원의 일정 목록 조회
     */
    public List<RouteListItemDto> getRoutesByMember(Long memberId) {
        // Repository에서 user_id로 조회하는 메서드를 호출
        List<Route> routes = routeRepository.findAllByUser_Id(memberId);

        // List<Route> -> List<RouteListItemDto> 변환
        return routes.stream()
                .map(RouteListItemDto::new) // DTO 생성자에서 엔티티를 받아서 처리한다고 가정
                .collect(Collectors.toList());
    }

    /**
     * 일정 수정
     */
    @Transactional
    public void updateRoute(Long routeId, RouteCreateRequestDto dto) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정이 없습니다. id=" + routeId));

        // Dirty Checking (변경 감지) - save() 호출 없이 값만 바꾸면 트랜잭션 종료 시 자동 업데이트
        route.setTitle(dto.getTitle());
        route.setStartDate(dto.getStartDate());
        route.setEndDate(dto.getEndDate());
        
        // 유저 변경이 필요한 경우 등은 추가 로직 작성
    }

    /**
     * 일정 삭제
     */
    @Transactional
    public void deleteRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정이 없습니다. id=" + routeId));

        routeRepository.delete(route);
    }
}