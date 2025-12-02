package com.example.project.route.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 생성/수정 시 사용하는 요청 DTO
 */
@Getter
@Setter
public class RouteCreateRequestDto {

    private Long memberId;      // 만든 사람 ID
    private String title;       // 일정 제목
    private LocalDate startDate;
    private LocalDate endDate;

    // 하루별 장소 정보 리스트
    private List<RoutePlaceRequestDto> places;
}
