package com.example.project.member.domain;

import java.time.LocalDate;

import com.example.project.route.domain.Route;
import jakarta.persistence.*;
import lombok.*;

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
    private TravelUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rt_id", nullable = false)
    private Route route;

    @Builder.Default
    @Column(name = "like_date")
    private LocalDate likeDate = LocalDate.now();
}
