package com.example.project.review.controller;

import com.example.project.review.dto.ReviewCreateRequestDto;
import com.example.project.review.dto.ReviewResponseDto;
import com.example.project.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * /api/review 하위의 리뷰(여행톡) 기능을 제공하는 컨트롤러.
 *
 * 기능:
 *  - 리뷰 작성
 *  - 일정별/일차별 리뷰 목록 조회
 *  - 리뷰 수정
 *  - 리뷰 삭제
 *
 * 회원 정보는 Security(로그인)에서 관리하며
 * 컨트롤러에서는 Principal을 통해 현재 로그인한 유저 정보를 읽어온다.
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
    public Long createReview(@RequestBody ReviewCreateRequestDto dto,
                             Principal principal) {
        return reviewService.createReview(dto, principal);
    }

    /**
     * 특정 일정 + 특정 일차 리뷰 조회
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
                             @RequestBody ReviewCreateRequestDto dto,
                             Principal principal) {
        reviewService.updateReview(reviewId, dto, principal);
    }

    /**
     * 리뷰 삭제
     * DELETE /api/review/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId,
                             Principal principal) {
        reviewService.deleteReview(reviewId, principal);
    }
}
