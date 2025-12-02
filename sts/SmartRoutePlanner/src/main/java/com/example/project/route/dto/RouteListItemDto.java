package com.example.project.route.dto;

import java.time.LocalDate;

import com.example.project.route.domain.Route;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 내 일정 목록 조회 시 한 줄에 나오는 정보 DTO
 */
@Getter
@Setter
@AllArgsConstructor
public class RouteListItemDto {

    private Long routeId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;

    public RouteListItemDto(Route route) {
        this.routeId = route.getId();
        this.title = route.getTitle();
        this.startDate = route.getStartDate();
        this.endDate = route.getEndDate();
    }

}
