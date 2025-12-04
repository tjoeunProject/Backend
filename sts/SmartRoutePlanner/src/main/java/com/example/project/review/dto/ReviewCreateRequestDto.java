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

    private Long routeId;   // 어떤 일정에 대한 리뷰인지
    private int dayIndex;   // 몇 일차 리뷰인지
    private String content; // 리뷰 내용
}
