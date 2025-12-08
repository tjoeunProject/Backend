package com.example.project.member.service;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.project.member.domain.MemberLikeRoute;
import com.example.project.member.domain.TravelUser;
import com.example.project.member.dto.UpdateProfileRequest;
import com.example.project.member.dto.UserInfoResponse;
import com.example.project.member.repository.MemberLikeRouteRepository;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.route.domain.Route;
import com.example.project.security.user.ChangePasswordRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TravelUserService {

    private final PasswordEncoder passwordEncoder;
    private final TravelUserRepository repository;
    private final MemberLikeRouteRepository likeRepository;

    // ================================
    // ❗ 1) 내 정보 조회
    // ================================
    public UserInfoResponse getMyInfo(Principal principal) {
        TravelUser user = repository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        return new UserInfoResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getAge(),
                user.getGender(),
                user.getRole()
        );
    }

    // ================================
    // ❗ 2) 프로필 수정 (닉네임, 성별, 나이)
    // ================================
    public TravelUser updateProfile(Principal principal, UpdateProfileRequest req) {

        TravelUser user = repository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        user.setNickname(req.getNickname());
        user.setGender(req.getGender());
        user.setAge(req.getAge());

        return repository.save(user);
    }

    // ================================
    // ❗ 3) 회원 탈퇴 (soft delete)
    // ================================
    public void deleteUser(Principal principal) {
        TravelUser user = repository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        user.setDelflag("Y");
        user.setDeletedate(LocalDate.now());
        repository.save(user);
    }

    // ================================
    // ❗ 4) 회원 복구
    // ================================
    public void restoreUser(Principal principal) {
        TravelUser user = repository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        user.setDelflag("N");
        user.setDeletedate(null);
        repository.save(user);
    }

    // ================================
    // ❗ 5) 좋아요한 루트 목록 조회
    // ================================
    public List<MemberLikeRoute> getMyLikedRoutes(Principal principal) {
        TravelUser user = repository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        return likeRepository.findByUser(user);
    }

    // ================================
    // ❗ 6) 내가 만든 루트 목록 조회
    // ================================
    public List<Route> getMyCreatedRoutes(Principal principal) {

        TravelUser user = repository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        return user.getRoutes();
    }

    // ================================
    // ❗❗❗ 수정 금지 — 원본 그대로 유지 ❗❗❗
    // ================================
    // 비번 변경
    
    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {

        var user = (TravelUser) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

        // check if the current password is correct
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalStateException("Wrong password");
        }
        // check if the two new passwords are the same
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new IllegalStateException("Password are not the same");
        }

        // update the password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // save the new password
        repository.save(user);
    }
}
