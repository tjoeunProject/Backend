package com.example.project.review.controller;

import com.example.project.review.dto.ReviewCreateRequestDto;
import com.example.project.review.dto.ReviewResponseDto;
import com.example.project.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Long createReview(@RequestBody ReviewCreateRequestDto dto,
                             Principal principal) {
        return reviewService.createReview(dto, principal);
    }

    @GetMapping("/{routeId}/{dayIndex}")
    public List<ReviewResponseDto> getReviews(
            @PathVariable Long routeId,
            @PathVariable int dayIndex) {
        return reviewService.getReviews(routeId, dayIndex);
    }

    @PutMapping("/{reviewId}")
    public void updateReview(@PathVariable Long reviewId,
                             @RequestBody ReviewCreateRequestDto dto,
                             Principal principal) {
        reviewService.updateReview(reviewId, dto, principal);
    }

    @DeleteMapping("/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId,
                             Principal principal) {
        reviewService.deleteReview(reviewId, principal);
    }
}
