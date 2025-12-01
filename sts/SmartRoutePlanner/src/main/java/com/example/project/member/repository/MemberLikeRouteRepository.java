package com.example.project.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.project.member.domain.MemberLikeRoute;

@Repository
public interface MemberLikeRouteRepository extends JpaRepository<MemberLikeRoute, Long>{

}
