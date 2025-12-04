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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TravelUserRepository travelUserRepository;
    private final RouteRepository routeRepository;

    public Long createReview(ReviewCreateRequestDto dto, Principal principal) {

        TravelUser user = travelUserRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보가 유효하지 않습니다."));

        Route route = routeRepository.findById(dto.getRouteId())
                .orElseThrow(() -> new IllegalArgumentException("없는 routeId: " + dto.getRouteId()));

        Review review = new Review();
        review.setRoute(route);
        review.setUser(user);
        review.setDayIndex(dto.getDayIndex());
        review.setContent(dto.getContent());

        Review saved = reviewRepository.save(review);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviews(Long routeId, int dayIndex) {

        return reviewRepository
                .findByRoute_IdAndDayIndexOrderByCreatedAtAsc(routeId, dayIndex)
                .stream()
                .map(ReviewResponseDto::new)
                .toList();
    }

    public void updateReview(Long reviewId, ReviewCreateRequestDto dto, Principal principal) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("없는 reviewId: " + reviewId));

        if (!review.getUser().getEmail().equals(principal.getName())) {
            throw new IllegalArgumentException("본인만 수정할 수 있습니다.");
        }

        review.setContent(dto.getContent());
    }

    public void deleteReview(Long reviewId, Principal principal) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("없는 reviewId: " + reviewId));

        if (!review.getUser().getEmail().equals(principal.getName())) {
            throw new IllegalArgumentException("본인만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
    }
}
