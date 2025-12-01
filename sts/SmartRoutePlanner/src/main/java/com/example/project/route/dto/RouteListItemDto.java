package com.example.project.route.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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
    private int totalDays;
}
