package com.example.project.review.repository;

import com.example.project.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Review 엔티티용 Repository.
 *
 * Route와 dayIndex를 기준으로 리뷰 목록을 조회하거나
 * 특정 Route에 속한 모든 리뷰를 삭제하는 기능을 제공한다.
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 특정 일정(Route)과 특정 일차에 대한 리뷰 목록을
     * 작성 시간 기준 오름차순으로 조회한다.
     */
    List<Review> findByRoute_IdAndDayIndexOrderByCreatedAtAsc(Long routeId, int dayIndex);

    /**
     * 특정 일정(Route)에 속한 리뷰 전체 삭제.
     * Route 삭제 시 함께 사용할 수 있다.
     */
    void deleteByRoute_Id(Long routeId);
}
