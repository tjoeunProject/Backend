package com.example.project.route.domain;

import com.example.project.place.domain.Place;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 하루(dayIndex) 안에서 어떤 장소(place)를 몇 번째(orderIndex)로 방문하는지 나타내는 엔티티
 */
@Entity
@Table(name = "route_place")
@Getter
@Setter
public class RoutePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    // 어떤 일정(Route)에 속하는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    // 어떤 장소(Place)를 가는지
    @JoinColumn(name = "place_id")
    private Long placeId;

 // ★ 추가 제안: 나중에 조회할 때 이름이 없으면 곤란하므로 이름도 같이 저장하는 게 좋습니다.
    @Column(name = "place_name") 
    private String placeName;
    
    private int dayIndex;    // 몇 일차인지 (1, 2, 3...)
    private int orderIndex;  // 그날 몇 번째로 방문하는지 (1, 2, 3...)
}
