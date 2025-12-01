package com.example.project.review.repository;

import com.example.project.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Review 엔티티용 Repository
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 일정 + 특정 일차의 리뷰 목록
    List<Review> findByRouteIdAndDayIndexOrderByCreatedAtAsc(Long routeId, int dayIndex);

    // 특정 일정 삭제 시 리뷰 싹 다 지울 때 사용 가능
    void deleteByRouteId(Long routeId);
}
