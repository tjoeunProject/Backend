package com.example.project.route.dto;

import com.example.project.place.dto.PlaceResponseDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 응답에서 사용하는 장소 요약 정보 DTO
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlaceSummaryDto extends PlaceResponseDto {

	private int orderIndex;
}
