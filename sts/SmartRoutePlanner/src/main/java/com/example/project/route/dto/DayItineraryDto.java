package com.example.project.route.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 특정 일차(dayIndex)에 대한 방문 장소 리스트 DTO
 */
@Getter
@Setter
@AllArgsConstructor
public class DayItineraryDto {

    private int dayIndex;
    private List<PlaceSummaryDto> places;
}
