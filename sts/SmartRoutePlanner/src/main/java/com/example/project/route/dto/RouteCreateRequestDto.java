package com.example.project.route.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class RouteCreateRequestDto {

    private Long memberId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<List<SimplePlaceDto>> places;

    @Data
    @NoArgsConstructor
    public static class SimplePlaceDto {
        private Long placeId;
        private String placeName;
    }
}
