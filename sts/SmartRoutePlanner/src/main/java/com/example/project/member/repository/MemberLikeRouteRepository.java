package com.example.project.member.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.project.member.domain.MemberLikeRoute;
import com.example.project.member.domain.TravelUser;

@Repository
public interface MemberLikeRouteRepository extends JpaRepository<MemberLikeRoute, Long> {

    List<MemberLikeRoute> findByUser(TravelUser user);

    boolean existsByUserIdAndRouteId(Integer userId, Long routeId);

    void deleteByUserIdAndRouteId(Integer userId, Long routeId);
}
