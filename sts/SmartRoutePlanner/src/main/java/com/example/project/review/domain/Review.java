package com.example.project.review.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 여행 일정(Route)의 특정 일자(dayIndex)에 대해
 * 멤버(memberId)가 남기는 리뷰(댓글) 엔티티
 */
@Entity
@Table(name = "review")
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;     // 리뷰 PK

    private Long routeId;   // 어떤 일정에 대한 리뷰인지

    private int dayIndex;   // 몇 일차의 리뷰인지

    private Long memberId;  // 작성자(멤버 ID)

    @Column(columnDefinition = "TEXT")
    private String content; // 댓글 내용

    @CreationTimestamp
    private LocalDateTime createdAt; // 작성 시간

    @UpdateTimestamp
    private LocalDateTime updatedAt; // 수정 시간
}
