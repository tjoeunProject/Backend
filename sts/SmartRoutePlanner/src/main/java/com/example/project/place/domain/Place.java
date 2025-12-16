package com.example.project.place.domain;

import java.util.List;
import com.example.project.route.domain.Route;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "place")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String googlePlaceId;
    private String name;
    private String formattedAddress;

    private double lat;
    private double lng;

    private double rating;
    private int userRatingsTotal;

    @ElementCollection 
    @CollectionTable(name = "place_types", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "type")
    private List<String> types;

    // [수정 포인트 1] @Lob 제거 + length 추가
    // Google Photo Reference는 길기 때문에 2000자 정도로 넉넉하게 잡습니다.
    @ElementCollection
    @CollectionTable(name = "place_photo", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "photo_reference", length = 2000) 
    private List<String> photoReferences;

    // [수정 포인트 2] @Lob 제거 + length 추가
    // HTML Attribution(출처) 링크도 길어질 수 있으므로 늘려줍니다.
    @ElementCollection
    @CollectionTable(name = "place_photo_attr", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "html_attr", length = 2000)
    private List<String> htmlAttributions;

    private int photoWidth;
    private int photoHeight;
    

}