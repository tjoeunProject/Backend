package com.example.project.member.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.project.member.domain.MemberLikeRoute;
import com.example.project.member.domain.TravelUser;
import com.example.project.member.dto.UpdateProfileRequest;
import com.example.project.member.dto.UserInfoResponse;
import com.example.project.member.service.TravelUserService;
import com.example.project.route.domain.Route;
import com.example.project.security.user.ChangePasswordRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class TravelUserController {

    private final TravelUserService service;

    // =========================
    // 1) 내 정보 조회
    // =========================
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(Principal principal) {
        return ResponseEntity.ok(service.getMyInfo(principal));
    }

    // =========================
    // 2) 프로필 수정
    // =========================
    @PutMapping("/profile")
    public ResponseEntity<TravelUser> updateProfile(
            Principal principal,
            @RequestBody UpdateProfileRequest req) {

        return ResponseEntity.ok(service.updateProfile(principal, req));
    }

    // =========================
    // 3) 비밀번호 변경
    // =========================
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody ChangePasswordRequest req,
            Principal principal) {

        service.changePassword(req, principal);
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }

    // =========================
    // 4) 회원 탈퇴
    // =========================
    @PostMapping("/delete")
    public ResponseEntity<String> deleteUser(Principal principal) {
        service.deleteUser(principal);
        return ResponseEntity.ok("회원 탈퇴 처리되었습니다.");
    }

    // =========================
    // 5) 회원 복구
    // =========================
    @PostMapping("/restore")
    public ResponseEntity<String> restoreUser(Principal principal) {
        service.restoreUser(principal);
        return ResponseEntity.ok("회원 상태가 복구되었습니다.");
    }

    // =========================
    // 6) 좋아요한 루트 목록
    // =========================
    @GetMapping("/likes")
    public ResponseEntity<List<MemberLikeRoute>> getMyLikedRoutes(Principal principal) {
        return ResponseEntity.ok(service.getMyLikedRoutes(principal));
    }

    // =========================
    // 7) 내가 만든 루트 목록
    // =========================
    @GetMapping("/routes")
    public ResponseEntity<List<Route>> getMyCreatedRoutes(Principal principal) {
        return ResponseEntity.ok(service.getMyCreatedRoutes(principal));
    }
}
