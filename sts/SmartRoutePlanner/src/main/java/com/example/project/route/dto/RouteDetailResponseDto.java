package com.example.project.route.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

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
