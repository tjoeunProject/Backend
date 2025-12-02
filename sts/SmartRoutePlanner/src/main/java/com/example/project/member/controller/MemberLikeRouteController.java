package com.example.project.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.project.member.service.MemberLikeRouteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
public class MemberLikeRouteController {

    private final MemberLikeRouteService likeService;

    // 좋아요 추가
    @PostMapping("/routes/{routeId}/likes")
    public ResponseEntity<?> like(
            @PathVariable Integer userId,
            @PathVariable Long routeId
    ) {
        return ResponseEntity.ok(likeService.like(userId, routeId));
    }

    // 좋아요 취소
    @DeleteMapping("/routes/{routeId}/likes")
    public ResponseEntity<?> unlike(
            @PathVariable Integer userId,
            @PathVariable Long routeId
    ) {
        likeService.unlike(userId, routeId);
        return ResponseEntity.ok().build();
    }

    // 좋아요 목록 조회
    @GetMapping("/likes")
    public ResponseEntity<?> getLikedRoutes(@PathVariable Integer userId) {
        return ResponseEntity.ok(likeService.getLikedRoutes(userId));
    }
}
