package com.example.project.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.project.member.domain.TravelUser;

public interface TravelUserRepository extends JpaRepository<TravelUser, Integer> {
    Optional<TravelUser> findByEmail(String email);
}
