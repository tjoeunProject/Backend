/**
 * PlaceResponseDto
 * ----------------------------
 * Place 엔티티의 응답 전용 DTO.
 * 엔티티 직접 노출을 방지하고,
 * API 응답 스키마를 안정적으로 유지하기 위해 사용한다.
 */

package com.example.project.place.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlaceResponseDto {

    private Long id;
    private String googlePlaceId;
    private String name;
    private String formattedAddress;

    private double lat;
    private double lng;

    private double rating;
    private int userRatingsTotal;

    private List<String> types;

    private List<String> photoReferences;
    private List<String> htmlAttributions;

    private int photoWidth;
    private int photoHeight;
}
