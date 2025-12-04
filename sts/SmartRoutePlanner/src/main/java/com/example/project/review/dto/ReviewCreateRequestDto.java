package com.example.project.review.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 리뷰 생성/수정 요청 DTO.
 *
 * 주의:
 *  - memberId는 클라이언트에서 받지 않는다.
 *  - 로그인된 회원 정보는 Principal에서 읽어온다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReviewCreateRequestDto {

    private Long routeId;
    private int dayIndex;
    private String content;
}
