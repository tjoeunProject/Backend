package com.example.project.review.repository;

import com.example.project.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRoute_IdAndDayIndexOrderByCreatedAtAsc(Long routeId, int dayIndex);

    void deleteByRoute_Id(Long routeId);
}
