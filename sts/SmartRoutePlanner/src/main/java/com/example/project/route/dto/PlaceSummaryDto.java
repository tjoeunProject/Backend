package com.example.project.route.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 응답에서 사용하는 장소 요약 정보 DTO
 */
@Getter
@Setter
@AllArgsConstructor
public class PlaceSummaryDto {

    private Long placeId;
    private String name;
    private int orderIndex;
}
