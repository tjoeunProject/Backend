package com.example.project.route.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 일정 생성/수정 시, 하루별 장소 정보를 담는 DTO
 */
@Getter
@Setter
public class RoutePlaceRequestDto {

    private int dayIndex;   // 몇 일차인지
    private Long placeId;   // Place 엔티티의 ID
    private int orderIndex; // 순서
}
