package com.example.project.review.dto;

import com.example.project.review.domain.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 리뷰 조회 응답 DTO.
 *
 * 기본 정보 외에 작성자의 닉네임을 함께 내려준다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDto {

    private Long reviewId;
    private Long memberId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Review 엔티티를 기반으로 DTO를 생성하는 편의 생성자.
     */
    public ReviewResponseDto(Review review) {
        this.reviewId = review.getId();
        if (review.getUser() != null && review.getUser().getId() != null) {
            this.memberId = review.getUser().getId().longValue();
            this.nickname = review.getUser().getNickname();
        }
        this.content = review.getContent();
        this.createdAt = review.getCreatedAt();
        this.updatedAt = review.getUpdatedAt();
    }
}
