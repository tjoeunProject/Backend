package com.example.project.route.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.project.route.domain.RoutePlace;

/**
 * RoutePlace 엔티티용 JPA Repository
 */
public interface RoutePlaceRepository extends JpaRepository<RoutePlace, Long> {


	// [변경 전]
    // List<RoutePlace> findByRouteIdOrderByDayIndexAscOrderIndexAsc(Long routeId);

    // [변경 후] JOIN FETCH 사용
	// 일정 조회 1번 + 장소 조회 N번 발생. 수정 후: 일정+장소 한 방에 조회.
    @Query("SELECT rp FROM RoutePlace rp JOIN FETCH rp.place WHERE rp.route.id = :routeId ORDER BY rp.dayIndex ASC, rp.orderIndex ASC")
    List<RoutePlace> findByRouteIdOrderByDayIndexAscOrderIndexAsc(@Param("routeId") Long routeId);
    
    /**
     * 특정 일정에 속한 모든 RoutePlace 삭제 (일정 수정/삭제 시 사용)
     */
    void deleteByRouteId(Long routeId);
    
    Optional<RoutePlace> findFirstByRouteIdOrderByDayIndexAscOrderIndexAsc(Long routeId);

    @Query("""
        select distinct rp from RoutePlace rp
        join fetch rp.place p left join fetch p.photoReferences
        where rp.route.id in :routeIds and not exists (
          select earlier.id from RoutePlace earlier where earlier.route.id = rp.route.id
          and (earlier.dayIndex < rp.dayIndex
            or (earlier.dayIndex = rp.dayIndex and earlier.orderIndex < rp.orderIndex)
            or (earlier.dayIndex = rp.dayIndex and earlier.orderIndex = rp.orderIndex and earlier.id < rp.id))
        )
        """)
    List<RoutePlace> findFirstPlacesForRoutes(@Param("routeIds") List<Long> routeIds);
}
