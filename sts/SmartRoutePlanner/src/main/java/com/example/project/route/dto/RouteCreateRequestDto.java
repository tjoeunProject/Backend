package com.example.project.route.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * RouteCreateRequestDto
 * ---------------------------------------
 * 일정 생성 요청에서 사용되는 DTO.
 *
 * 구조:
 *  - memberId: 작성한 유저 ID
 *  - title: 제목
 *  - startDate, endDate: 일정 기간
 *  - places: 2차원 리스트 (일자별 장소 목록)
 *
 * 예시:
 * [
 *   [ {placeId: 1}, {placeId: 2} ],   // 1일차
 *   [ {placeId: 3} ],                 // 2일차
 *   [ {placeId: 4}, {placeId: 5} ]    // 3일차
 * ]
 */
@Data
@NoArgsConstructor
public class RouteCreateRequestDto {

    private Long memberId;                 // 일정 작성자
    private String title;                  // 일정 제목
    private LocalDate startDate;           // 시작일
    private LocalDate endDate;             // 종료일

    /**
     * 2차원 배열 형태의 일정 정보 (일자별 장소 목록)
     * 예: places.get(0) = 1일차 리스트
     */
    private List<List<SimplePlaceDto>> places;

    /**
     * 일정 생성 시 장소 하나를 표현하는 DTO
     */
    @Data
    @NoArgsConstructor
    public static class SimplePlaceDto {
        private Long placeId;
        private String placeName;
    }
}
