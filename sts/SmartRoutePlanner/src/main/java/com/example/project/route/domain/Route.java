package com.example.project.route.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 여행 일정(Route)의 기본 정보 엔티티
 * - 제목, 기간, 만든 사람, 총 일수 등
 */
@Entity
@Table(name = "route")
@Getter
@Setter
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;            // 일정 ID (PK)

    private Long memberId;      // 일정을 만든 회원 ID (추후 Member 엔티티와 연관 가능)

    private String title;       // 일정 제목 (예: 부산 2박 3일 여행)

    private LocalDate startDate; // 시작 날짜
    private LocalDate endDate;   // 종료 날짜

    private int totalDays;      // 총 일수 (endDate - startDate + 1 계산 결과)

    // Route 1개가 여러 RoutePlace(하루별 장소 목록)를 가짐
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutePlace> routePlaces = new ArrayList<>();
}