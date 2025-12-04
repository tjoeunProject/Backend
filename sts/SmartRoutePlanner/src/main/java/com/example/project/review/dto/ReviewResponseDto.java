package com.example.project.review.dto;

import com.example.project.review.domain.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    public ReviewResponseDto(Review review) {
        this.reviewId = review.getId();
        if (review.getUser() != null) {
            this.memberId = review.getUser().getId().longValue();
            this.nickname = review.getUser().getNickname();
        }
        this.content = review.getContent();
        this.createdAt = review.getCreatedAt();
        this.updatedAt = review.getUpdatedAt();
    }
}
