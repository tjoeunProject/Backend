package com.example.project.route.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
// 1. TravelUser 임포트 필수! (패키지 경로는 본인 프로젝트에 맞게 확인해주세요)
import com.example.project.member.domain.TravelUser; 

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "route")
@Getter
@Setter
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;            // 일정 ID (PK)

    // ================= [수정된 부분] =================
    // 기존: private Long memberId; (단순 숫자값 X)
    
    // 변경: 객체 관계 매핑 (변수 이름을 반드시 'user'로 해야 mappedBy="user"와 연결됨)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id") // DB 테이블에 생길 컬럼 이름 (예: MEMBER_ID)
    private TravelUser user; 
    // ===============================================

    private String title;       // 일정 제목

    private LocalDate startDate; // 시작 날짜
    private LocalDate endDate;   // 종료 날짜

    private int totalDays;      // 총 일수

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutePlace> routePlaces = new ArrayList<>();
}