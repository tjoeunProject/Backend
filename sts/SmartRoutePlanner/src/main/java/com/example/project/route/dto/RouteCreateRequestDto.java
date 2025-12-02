package com.example.project.route.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor

public class RouteCreateRequestDto {
    private Long memberId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;

    // ★ 핵심 변경: 이중 리스트 (List<List<...>>)
    // 예: [ [1일차장소A, 1일차장소B], [2일차장소C], [3일차장소D, 3일차장소E] ]
    // 배열의 '첫 번째 줄'이 1일차, 그 줄의 '첫 번째 칸'이 1번 순서가 됩니다.
    private List<List<SimplePlaceDto>> places; 

    // 내부 static 클래스로 간단하게 정의 (별도 파일로 만들어도 됨)
    @Data
    public static class SimplePlaceDto {
        private Long placeId; // 장소 ID
        private String placeName; // 필요하면 이름도 받음
    }
}