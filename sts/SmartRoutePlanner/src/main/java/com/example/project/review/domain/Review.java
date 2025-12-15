package com.example.project.review.domain;

import com.example.project.member.domain.TravelUser;
import com.example.project.route.domain.Route;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberid", nullable = false)
    private TravelUser user;

    private int dayIndex;

    @Lob
    private String content;

    /** 작성 시간 */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** 수정 시간 */
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
