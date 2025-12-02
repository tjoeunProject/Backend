package com.example.project.review.service;

import com.example.project.review.domain.Review;
import com.example.project.review.dto.ReviewCreateRequestDto;
import com.example.project.review.dto.ReviewResponseDto;
import com.example.project.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 리뷰 생성, 조회, 수정, 삭제를 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;

    /**
     * 리뷰 작성
     */
    public Long createReview(ReviewCreateRequestDto dto) {

        Review review = new Review();
        review.setRouteId(dto.getRouteId());
        review.setDayIndex(dto.getDayIndex());
        review.setMemberId(dto.getMemberId());
        review.setContent(dto.getContent());

        Review saved = reviewRepository.save(review);
        return saved.getId();
    }

    /**
     * 특정 일정의 특정 일자 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviews(Long routeId, int dayIndex) {

        return reviewRepository
                .findByRouteIdAndDayIndexOrderByCreatedAtAsc(routeId, dayIndex)
                .stream()
                .map(r -> new ReviewResponseDto(
                        r.getId(),
                        r.getMemberId(),
                        r.getContent(),
                        r.getCreatedAt(),
                        r.getUpdatedAt()))
                .toList();
    }

    /**
     * 리뷰 수정
     */
    public void updateReview(Long reviewId, ReviewCreateRequestDto dto) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("없는 reviewId: " + reviewId));

        review.setContent(dto.getContent());
    }

    /**
     * 리뷰 삭제
     */
    public void deleteReview(Long reviewId) {

        if (!reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("없는 reviewId: " + reviewId);
        }

        reviewRepository.deleteById(reviewId);
    }
}
