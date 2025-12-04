package com.example.project.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.project.member.domain.TravelUser;

@Repository
public interface TravelUserRepository extends JpaRepository<TravelUser, Integer>{

	public Optional<TravelUser> findByEmail(String username);

	public Optional<TravelUser> findById(Long memberId);

}
