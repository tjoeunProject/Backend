package com.example.project.route.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.project.member.domain.TravelUser;
import com.example.project.place.domain.Place;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "route")
@Getter
@Setter
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * TravelUser와의 연관관계
     * Route N : 1 User
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberid", nullable = false)
    private TravelUser user;

    private String title;

    private LocalDate startDate;
    private LocalDate endDate;

    private int totalDays;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    private List<RoutePlace> routePlaces = new ArrayList<>();
}
