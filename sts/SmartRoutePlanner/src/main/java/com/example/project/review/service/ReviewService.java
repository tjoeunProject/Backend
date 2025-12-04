package com.example.project.review.service;

import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.review.domain.Review;
import com.example.project.review.dto.ReviewCreateRequestDto;
import com.example.project.review.dto.ReviewResponseDto;
import com.example.project.review.repository.ReviewRepository;
import com.example.project.route.domain.Route;
import com.example.project.route.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

/**
 * 리뷰 생성, 조회, 수정, 삭제를 처리하는 서비스.
 *
 * 회원 정보는 Security(Principal)에서 가져오며,
 * 작성자 본인 검증 후에만 수정/삭제가 가능하도록 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RouteRepository routeRepository;
    private final TravelUserRepository travelUserRepository;

    /**
     * 리뷰 작성.
     *
     * 1) Principal에서 이메일을 꺼내 회원 조회
     * 2) routeId로 Route 조회
     * 3) Review 엔티티 생성 후 저장
     */
    @Transactional
    public Long createReview(ReviewCreateRequestDto dto, Principal principal) {

        // 로그인한 사용자 조회
        String email = principal.getName();
        TravelUser user = travelUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. email=" + email));

        // 일정(Route) 조회
        Route route = routeRepository.findById(dto.getRouteId())
                .orElseThrow(() -> new IllegalArgumentException("일정 정보를 찾을 수 없습니다. routeId=" + dto.getRouteId()));

        Review review = new Review();
        review.setRoute(route);
        review.setUser(user);
        review.setDayIndex(dto.getDayIndex());
        review.setContent(dto.getContent());

        Review saved = reviewRepository.save(review);
        return saved.getId();
    }

    /**
     * 특정 일정의 특정 일자 리뷰 목록 조회.
     */
    public List<ReviewResponseDto> getReviews(Long routeId, int dayIndex) {

        return reviewRepository
                .findByRoute_IdAndDayIndexOrderByCreatedAtAsc(routeId, dayIndex)
                .stream()
                .map(ReviewResponseDto::new)
                .toList();
    }

    /**
     * 리뷰 수정.
     *
     * 작성자 본인인지 확인 후 내용만 수정한다.
     */
    @Transactional
    public void updateReview(Long reviewId,
                             ReviewCreateRequestDto dto,
                             Principal principal) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("없는 reviewId: " + reviewId));

        TravelUser currentUser = getCurrentUser(principal);

        // 작성자 검증
        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        review.setContent(dto.getContent());
    }

    /**
     * 리뷰 삭제.
     *
     * 작성자 본인만 삭제 가능하며, 삭제 방식은 완전 삭제이다.
     */
    @Transactional
    public void deleteReview(Long reviewId, Principal principal) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("없는 reviewId: " + reviewId));

        TravelUser currentUser = getCurrentUser(principal);

        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
    }

    /**
     * Principal에서 이메일을 꺼내 현재 로그인한 회원을 조회하는 공통 메서드.
     */
    private TravelUser getCurrentUser(Principal principal) {
        String email = principal.getName();
        return travelUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. email=" + email));
    }
}
