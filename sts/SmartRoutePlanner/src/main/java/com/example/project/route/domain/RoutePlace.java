package com.example.project.route.domain;

import com.example.project.place.domain.Place;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * RoutePlace
 * ---------------------------------------
 * 하나의 일정(Route) 안에서 특정 장소(Place)를
 * 몇 일차(dayIndex), 몇 번째(orderIndex)에 방문하는지 기록하는 엔티티.
 *
 * 역할:
 *  - Route 1개에는 여러 RoutePlace가 연결됨
 *  - 각 RoutePlace는 특정 Place와 연결됨
 *  - dayIndex: 몇 일차인지
 *  - orderIndex: 해당 날짜에서 방문 순서
 */
@Entity
@Table(name = "route_place")
@Getter
@Setter
public class RoutePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 RoutePlace가 속한 일정 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    /** 방문하는 장소 (Place 엔티티) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    /** 조회 속도 향상을 위해 placeName도 저장 */
    @Column(name = "place_name")
    private String placeName;

    /** 몇 일차인지 (1일부터 시작) */
    private int dayIndex;

    /** 해당 날짜에서 몇 번째 방문인지 */
    private int orderIndex;
}
