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
     * 리뷰 작성
     */
    @Transactional
    public Long createReview(ReviewCreateRequestDto dto, Principal principal) {

        // 로그인 사용자 조회
        TravelUser user = getCurrentUser(principal);

        // 일정(Route) 조회
        Route route = routeRepository.findById(dto.getRouteId())
                .orElseThrow(() -> new IllegalArgumentException("일정 정보를 찾을 수 없습니다. routeId=" + dto.getRouteId()));

        // 리뷰 엔티티 생성
        Review review = new Review();
        review.setRoute(route);
        review.setUser(user);
        review.setDayIndex(dto.getDayIndex());
        review.setContent(dto.getContent());

        // 저장
        Review saved = reviewRepository.save(review);
        return saved.getId();
    }

    /**
     * 특정 일정 + 특정 일차 리뷰 조회
     */
    public List<ReviewResponseDto> getReviews(Long routeId, int dayIndex) {
        return reviewRepository
                .findByRoute_IdAndDayIndexOrderByCreatedAtAsc(routeId, dayIndex)
                .stream()
                .map(ReviewResponseDto::new)
                .toList();
    }

    /**
     * 리뷰 수정 (본인만 가능)
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
     * 리뷰 삭제 (본인만 가능)
     */
    @Transactional
    public void deleteReview(Long reviewId, Principal principal) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("없는 reviewId: " + reviewId));

        TravelUser currentUser = getCurrentUser(principal);

        // 작성자 검증
        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
    }

    /**
     * Principal 기반 현재 로그인 유저 조회
     */
    private TravelUser getCurrentUser(Principal principal) {
        String email = principal.getName();
        return travelUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. email=" + email));
    }
}
