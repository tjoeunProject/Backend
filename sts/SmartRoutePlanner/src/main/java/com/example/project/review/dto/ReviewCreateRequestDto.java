package com.example.project.review.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 리뷰 생성/수정 요청 DTO
 */
@Getter
@Setter
public class ReviewCreateRequestDto {

    private Long routeId;
    private int dayIndex;
    private Long memberId;
    private String content;
}
