package com.example.project.member.dto;

import java.time.LocalDate;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeRouteResponse {

    private Long likeId;          // 좋아요 ID
    private Long routeId;         // 루트 ID
    private String routeName;     // 루트 이름
    private LocalDate likeDate;   // 좋아요 누른 날짜
}
