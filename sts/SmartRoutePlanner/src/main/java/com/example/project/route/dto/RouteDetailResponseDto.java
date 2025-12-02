package com.example.project.route.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

import com.example.project.route.domain.Route;

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

    private List<DayItineraryDto> days; // 하루별 일정 리스트
    
    public RouteDetailResponseDto(Route route) {
        this.routeId = route.getId();
        this.title = route.getTitle();
        this.startDate = route.getStartDate();
        this.endDate = route.getEndDate();
    }
}
