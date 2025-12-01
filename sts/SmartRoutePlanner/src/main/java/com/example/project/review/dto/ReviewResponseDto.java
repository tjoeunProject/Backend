package com.example.project.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 리뷰 조회 응답 DTO
 */
@Getter
@Setter
@AllArgsConstructor
public class ReviewResponseDto {

    private Long reviewId;
    private Long memberId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
