package com.example.project.route.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 상세 조회 응답 DTO
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
    private int totalDays;

    private List<DayItineraryDto> days; // 하루별 일정 리스트
}
