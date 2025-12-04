package com.example.project.route.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * RouteDetailResponseDto
 * ---------------------------------------
 * 일정 상세 조회 응답 DTO.
 *
 * 포함 정보:
 *  - routeId: 일정 PK
 *  - memberId: 작성자 ID
 *  - title, startDate, endDate
 *  - days: DayItineraryDto 리스트 (일자별 장소 리스트)
 */
@Getter
@Setter
@AllArgsConstructor
public class RouteDetailResponseDto {

    private Long routeId;
    private Long memberId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<DayItineraryDto> days;
}
