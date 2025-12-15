package com.example.project.place.domain;

import java.util.List;

import com.example.project.route.domain.Route;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "place")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // 내부 DB PK

    private String googlePlaceId;      // 구글 place_id
    private String name;               // 이름
    private String formattedAddress;   // 주소

    private double lat;                // 위도
    private double lng;                // 경도

    private double rating;             // 평점
    private int userRatingsTotal;      // 리뷰 수

    @ElementCollection 
    @CollectionTable(name = "place_types", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "type")
    private List<String> types;        // types 배열 저장

    // photoReference 여러 개일 수도 있으니 List<String>
    @ElementCollection
    @CollectionTable(name = "place_photo", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "photo_reference")
    private List<String> photoReferences;

    // html_attributions - 사용 여부 선택 가능
    @ElementCollection
    @CollectionTable(name = "place_photo_attr", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "html_attr")
    private List<String> htmlAttributions;

    private int photoWidth;
    private int photoHeight;
    
    @ManyToOne(fetch = FetchType.LAZY) // 성능을 위해 LAZY 권장
    @JoinColumn(name = "route_id")     // 외래키 컬럼명 지정
    private Route route;               // 필드명이 mappedBy="route"와 일치해야 함
}
