package com.example.project.route.domain;

import com.example.project.member.domain.TravelUser;
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
    private Long id;

    /**
     * TravelUser와의 연관관계
     * Route N : 1 User
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberid", nullable = false)
    private TravelUser user;

    private String title;

    private LocalDate startDate;
    private LocalDate endDate;

    private int totalDays;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutePlace> routePlaces = new ArrayList<>();
}
