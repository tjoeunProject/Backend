package com.example.project.member.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.project.global.exception.AlreadyLikedException;
import com.example.project.global.exception.LikeNotFoundException;
import com.example.project.global.exception.RouteNotFoundException;
import com.example.project.global.exception.UserNotFoundException;
import com.example.project.member.domain.MemberLikeRoute;
import com.example.project.member.domain.TravelUser;
import com.example.project.member.dto.LikeRouteResponse;
import com.example.project.member.repository.MemberLikeRouteRepository;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.route.domain.Route;
import com.example.project.route.repository.RouteRepository;

import lombok.RequiredArgsConstructor;
import com.example.project.security.config.CurrentUser;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberLikeRouteService {

    private final MemberLikeRouteRepository likeRepo;
    private final TravelUserRepository userRepo;
    private final RouteRepository routeRepo;
    private final CurrentUser currentUser;

    // ============================================================
    // 좋아요 추가
    // ============================================================
    public MemberLikeRoute like(Integer userId, Long routeId) {

        TravelUser user = currentUser.requireId(userId);

        Route route = routeRepo.findById(routeId)
                .orElseThrow(RouteNotFoundException::new);

        // 중복 체크
        if (likeRepo.existsByUserIdAndRouteId(userId, routeId)) {
            throw new AlreadyLikedException();
        }

        MemberLikeRoute like = MemberLikeRoute.builder()
                .user(user)
                .route(route)
                .build();

        return likeRepo.save(like);
    }

    // ============================================================
    // 좋아요 취소
    // ============================================================
    public void unlike(Integer userId, Long routeId) {
        currentUser.requireId(userId);

        if (!likeRepo.existsByUserIdAndRouteId(userId, routeId)) {
            throw new LikeNotFoundException();
        }

        likeRepo.deleteByUserIdAndRouteId(userId, routeId);
    }

    // ============================================================
    // 좋아요 목록 (엔티티 그대로)
    // ============================================================
    public List<MemberLikeRoute> getLikedRoutes(Integer userId) {

        TravelUser user = currentUser.requireId(userId);

        return likeRepo.findByUser(user);
    }

    // ============================================================
    // 좋아요 목록 (DTO 변환 버전)
    // ============================================================
    public List<LikeRouteResponse> getLikedRoutesDto(Integer userId) {

        TravelUser user = currentUser.requireId(userId);

        List<MemberLikeRoute> likes = likeRepo.findByUser(user);

        return likes.stream()
                .map(like -> LikeRouteResponse.builder()
                        .likeId(like.getId())
                        .routeId(like.getRoute().getId())
                        .routeName(like.getRoute().getTitle())   // Route 엔티티의 제목
                        .likeDate(like.getLikeDate())
                        .build()
                ).toList();
    }
}
