package com.example.project.route.repository;

import com.example.project.route.domain.RoutePlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * RoutePlace 엔티티용 JPA Repository
 */
public interface RoutePlaceRepository extends JpaRepository<RoutePlace, Long> {

    /**
     * 특정 일정에 속한 장소들을 dayIndex, orderIndex 기준으로 정렬해서 조회
     */
    List<RoutePlace> findByRouteIdOrderByDayIndexAscOrderIndexAsc(Long routeId);

    /**
     * 특정 일정에 속한 모든 RoutePlace 삭제 (일정 수정/삭제 시 사용)
     */
    void deleteByRouteId(Long routeId);
}
