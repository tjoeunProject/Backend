package com.example.project.member.domain;

import java.time.LocalDate;

import org.springframework.util.RouteMatcher.Route;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "MEMBER_LIKE_ROUTE")
public class MemberLikeRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_like_id_gen")
    @SequenceGenerator(name = "seq_like_id_gen", sequenceName = "SEQ_LIKE_ID", allocationSize = 1)
    @Column(name = "like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberid", nullable = false)
    private TravelUser user; // TRAVEL_USER 참조

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rt_id", nullable = false)
    private Route route;

    @Builder.Default
    @Column(name = "like_date")
    private LocalDate likeDate = LocalDate.now();
}