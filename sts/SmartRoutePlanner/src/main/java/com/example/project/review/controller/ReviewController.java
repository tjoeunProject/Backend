package com.example.project.review.controller;

import com.example.project.review.dto.ReviewCreateRequestDto;
import com.example.project.review.dto.ReviewResponseDto;
import com.example.project.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * /api/review 하위 리뷰 기능 제공
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 작성
     * POST /api/review
     */
    @PostMapping
    public Long createReview(@RequestBody ReviewCreateRequestDto dto) {
        return reviewService.createReview(dto);
    }

    /**
     * 특정 일정 + 특정 일차 리뷰 조회
     * GET /api/review/{routeId}/{dayIndex}
     */
    @GetMapping("/{routeId}/{dayIndex}")
    public List<ReviewResponseDto> getReviews(
            @PathVariable Long routeId,
            @PathVariable int dayIndex) {

        return reviewService.getReviews(routeId, dayIndex);
    }

    /**
     * 리뷰 수정
     * PUT /api/review/{reviewId}
     */
    @PutMapping("/{reviewId}")
    public void updateReview(@PathVariable Long reviewId,
                             @RequestBody ReviewCreateRequestDto dto) {
        reviewService.updateReview(reviewId, dto);
    }

    /**
     * 리뷰 삭제
     * DELETE /api/review/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
    }
}
