package com.example.project.route.repository;

import com.example.project.route.domain.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Route 엔티티용 JPA Repository
 */
public interface RouteRepository extends JpaRepository<Route, Long> {

    /**
     * 특정 회원이 만든 일정 목록 조회
     * TravelUser의 PK는 Integer라서 Integer 타입을 사용한다.
     */
    List<Route> findAllByUser_Id(Integer memberId);
}
