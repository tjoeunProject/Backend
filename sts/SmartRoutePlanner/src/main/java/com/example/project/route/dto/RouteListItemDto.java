package com.example.project.route.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.project.place.domain.Place;
import com.example.project.route.domain.Route;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 내 일정 목록 조회 시 한 줄에 나오는 정보 DTO
 */
@Getter
@Setter
@AllArgsConstructor
public class RouteListItemDto {

    private Long routeId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;

    private String mainPlaceName;      // 첫 번째 장소 이름 (예: 인천국제공항)
    private String photoUrl; // 첫 번째 장소의 사진 참조값 (구글 이미지 API용)
    
    /**
     * Route 엔티티를 편하게 DTO로 변환하기 위한 생성자
     */
    public RouteListItemDto(Route route, Place firstPlace) {
        this.routeId = route.getId();
        this.title = route.getTitle();
        this.startDate = route.getStartDate();
        this.endDate = route.getEndDate();
        this.totalDays = route.getTotalDays();
        
     // 🔥 [추가된 로직] 첫 번째 장소 정보가 있으면 채워넣기
        if (firstPlace != null) {
            this.mainPlaceName = firstPlace.getName();

            // 사진 목록이 있고 비어있지 않다면 첫 번째 사진 가져오기
            List<String> photos = firstPlace.getPhotoReferences();
            if (photos != null && !photos.isEmpty()) {
                this.photoUrl = photos.get(0);
            }
        } else {
            this.mainPlaceName = "장소 없음";
            this.photoUrl = null;
        }
    }
    
    
    
    
}
